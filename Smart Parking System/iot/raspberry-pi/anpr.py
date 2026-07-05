import cv2
import numpy as np
import onnxruntime as ort
import easyocr
import logging

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger("ANPR")

class ANPREngine:
    def __init__(self, model_path):
        logger.info(f"Loading YOLOv8 ONNX model from: {model_path}")
        # Initialize ONNX Runtime Session
        try:
            self.session = ort.InferenceSession(model_path, providers=['CPUExecutionProvider'])
            self.input_name = self.session.get_inputs()[0].name
            self.input_shape = self.session.get_inputs()[0].shape # e.g. [1, 3, 640, 640]
            self.input_width = self.input_shape[2]
            self.input_height = self.input_shape[3]
            logger.info("ONNX model loaded successfully.")
        except Exception as e:
            logger.error(f"Failed to load ONNX model: {e}")
            raise e

        # Initialize EasyOCR Reader for English/Vietnamese alphanumeric characters
        logger.info("Initializing EasyOCR Reader...")
        self.reader = easyocr.Reader(['en'], gpu=False)
        logger.info("EasyOCR Reader initialized.")

    def detect_plate(self, frame):
        """
        Detects license plates in the frame using YOLOv8 ONNX.
        Returns a cropped image of the best plate found, along with its bounding box coordinates.
        """
        h_orig, w_orig = frame.shape[:2]

        # Preprocessing: resize to 640x640
        input_image = cv2.resize(frame, (self.input_width, self.input_height))
        # Convert BGR to RGB
        input_image = cv2.cvtColor(input_image, cv2.COLOR_BGR2RGB)
        # Normalize to [0, 1]
        input_image = input_image.astype(np.float32) / 255.0
        # Transpose from HWC to CHW
        input_image = input_image.transpose(2, 0, 1)
        # Add Batch dimension: (1, 3, 640, 640)
        blob = np.expand_dims(input_image, axis=0)

        # Run inference
        outputs = self.session.run(None, {self.input_name: blob})
        output = outputs[0]  # Shape: (1, 5, 8400)
        output = np.squeeze(output)  # Shape: (5, 8400)
        output = output.T  # Shape: (8400, 5) -> rows of [x_center, y_center, w, h, score]

        # Scaling factors
        x_factor = w_orig / self.input_width
        y_factor = h_orig / self.input_height

        boxes = []
        confidences = []

        # Filter output bounding boxes by score threshold
        for row in output:
            score = row[4]
            if score > 0.4:
                x_center, y_center, w, h = row[0], row[1], row[2], row[3]
                
                # Convert to top-left coordinate boxes
                x = int((x_center - w/2) * x_factor)
                y = int((y_center - h/2) * y_factor)
                width = int(w * x_factor)
                height = int(h * y_factor)
                
                # Keep box within frame boundaries
                x = max(0, x)
                y = max(0, y)
                width = min(w_orig - x, width)
                height = min(h_orig - y, height)

                boxes.append([x, y, width, height])
                confidences.append(float(score))

        if not boxes:
            return None, None

        # Apply Non-Maximum Suppression (NMS) to eliminate duplicate boxes
        indices = cv2.dnn.NMSBoxes(boxes, confidences, 0.4, 0.45)
        
        if len(indices) == 0:
            return None, None

        # Get the box with the highest confidence
        best_idx = indices[0]
        if isinstance(best_idx, (list, np.ndarray)):
            best_idx = best_idx[0]

        best_box = boxes[best_idx]
        x, y, w, h = best_box
        
        # Crop plate area
        cropped_plate = frame[y:y+h, x:x+w]
        return cropped_plate, (x, y, w, h)

    def recognize_text(self, cropped_plate):
        """
        Performs OCR text extraction on the cropped license plate.
        Returns clean alphanumeric text string and OCR confidence.
        """
        if cropped_plate is None or cropped_plate.size == 0:
            return None, 0.0

        # Preprocessing crop for better OCR
        # Convert to grayscale
        gray = cv2.cvtColor(cropped_plate, cv2.COLOR_BGR2GRAY)
        
        # Upscale if the crop is small
        if gray.shape[0] < 50:
            gray = cv2.resize(gray, (0, 0), fx=2.0, fy=2.0, interpolation=cv2.INTER_CUBIC)

        # Run OCR
        results = self.reader.readtext(gray)
        
        if not results:
            return None, 0.0

        # Combine text from all boxes sorted from left to right
        results = sorted(results, key=lambda r: r[0][0][0])
        
        full_text = ""
        total_conf = 0.0
        
        for bbox, text, conf in results:
            # Clean non-alphanumeric chars (keep letter/digit)
            clean_text = "".join([c for c in text if c.isalnum()]).upper()
            full_text += clean_text
            total_conf += conf

        avg_conf = total_conf / len(results) if results else 0.0
        
        # Standardize Vietnamese plates (e.g. 29A12345 or 30F9876)
        # Remove any leading/trailing space
        full_text = full_text.strip().replace(" ", "")
        
        return full_text, avg_conf
