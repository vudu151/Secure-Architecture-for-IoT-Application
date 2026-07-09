import cv2
import numpy as np
import logging
import os

logger = logging.getLogger("ANPR")


class ANPREngine:
    def __init__(self, hf_repo: str, hf_filename: str, cache_dir: str):
        import easyocr
        from huggingface_hub import hf_hub_download
        from ultralytics import YOLO

        logger.info("Initializing EasyOCR...")
        ocr_model_dir = os.path.join(os.path.dirname(__file__), "models", "ocr")
        os.makedirs(ocr_model_dir, exist_ok=True)

        self.reader = easyocr.Reader(
            ["en"],
            gpu=False,
            model_storage_directory=ocr_model_dir,
            download_enabled=True,
        )
        logger.info("EasyOCR ready.")

        os.makedirs(cache_dir, exist_ok=True)
        local_pt = os.path.join(cache_dir, hf_filename)

        if not os.path.exists(local_pt):
            logger.info(f"Downloading {hf_repo}/{hf_filename} from HuggingFace...")
            local_pt = hf_hub_download(
                repo_id=hf_repo,
                filename=hf_filename,
                local_dir=cache_dir,
            )
            logger.info(f"Model saved to {local_pt}")
        else:
            logger.info(f"Using cached model: {local_pt}")

        self.model = YOLO(local_pt)
        logger.info("YOLOv8 license plate detector loaded.")

    def detect_and_recognize(self, frame) -> tuple[str | None, float]:
        plate_crop = self._detect_plate(frame)
        if plate_crop is not None:
            result = self._recognize_text(plate_crop)
            if result[0]:
                return result
        return self._recognize_text(frame)

    def _detect_plate(self, frame):
        results = self.model.predict(frame, conf=0.1, verbose=False)
        if not results or results[0].boxes is None or len(results[0].boxes) == 0:
            return None

        boxes = results[0].boxes
        confs = boxes.conf.cpu().numpy()
        best_idx = int(np.argmax(confs))
        x1, y1, x2, y2 = boxes.xyxy[best_idx].cpu().numpy().astype(int)

        h, w = frame.shape[:2]
        pad_x = int((x2 - x1) * 0.2)
        pad_y = int((y2 - y1) * 0.2)
        x1, y1 = max(0, x1 - pad_x), max(0, y1 - pad_y)
        x2, y2 = min(w, x2 + pad_x), min(h, y2 + pad_y)

        crop = frame[y1:y2, x1:x2]
        if crop.size == 0:
            return None
        return crop

    def _recognize_text(self, crop) -> tuple[str | None, float]:
        if crop is None or crop.size == 0:
            return None, 0.0

        # Enlarge the image if it is too small
        target_height = 100
        if crop.shape[0] < target_height:
            scale = target_height / crop.shape[0]
            crop = cv2.resize(
                crop, (0, 0), fx=scale, fy=scale, interpolation=cv2.INTER_CUBIC
            )

        gray = cv2.cvtColor(crop, cv2.COLOR_BGR2GRAY)

        # Apply Bilateral Filter to reduce noise but keep edges sharp
        blur = cv2.bilateralFilter(gray, 11, 17, 17)

        # Apply CLAHE to improve contrast for OCR
        clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8))
        enhanced = clahe.apply(blur)

        # Restrict characters to uppercase letters, digits, and standard plate punctuation
        allowlist = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-."
        results = self.reader.readtext(enhanced, allowlist=allowlist, paragraph=False)
        if not results:
            return None, 0.0

        # Detect 2-row plates by checking vertical spread of OCR bounding boxes
        centers_y = [((r[0][0][1] + r[0][2][1]) / 2) for r in results]
        y_span = max(centers_y) - min(centers_y) if len(centers_y) > 1 else 0
        crop_h = crop.shape[0]

        if y_span > crop_h * 0.3:
            median_y = (max(centers_y) + min(centers_y)) / 2
            top_row = sorted(
                [r for r, cy in zip(results, centers_y) if cy <= median_y],
                key=lambda r: r[0][0][0],
            )
            bot_row = sorted(
                [r for r, cy in zip(results, centers_y) if cy > median_y],
                key=lambda r: r[0][0][0],
            )

            def extract(row):
                return "".join(
                    "".join(c for c in r[1] if c.isalnum() or c in "-.").upper()
                    for r in row
                )

            top_text = extract(top_row)
            bot_text = extract(bot_row)
            full_text = f"{top_text}-{bot_text}" if top_text and bot_text else top_text or bot_text
        else:
            results = sorted(results, key=lambda r: r[0][0][0])
            full_text = "".join(
                "".join(c for c in text if c.isalnum() or c in "-.").upper()
                for _, text, _ in results
            )

        total_conf = sum(r[2] for r in results)
        avg_conf = total_conf / len(results)
        return full_text.strip() or None, avg_conf
