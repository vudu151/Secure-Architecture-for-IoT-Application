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
        if plate_crop is None:
            return None, 0.0
        return self._recognize_text(plate_crop)

    def _detect_plate(self, frame):
        results = self.model.predict(frame, conf=0.4, verbose=False)
        if not results or results[0].boxes is None or len(results[0].boxes) == 0:
            return None

        boxes = results[0].boxes
        confs = boxes.conf.cpu().numpy()
        best_idx = int(np.argmax(confs))
        x1, y1, x2, y2 = boxes.xyxy[best_idx].cpu().numpy().astype(int)

        h, w = frame.shape[:2]
        x1, y1 = max(0, x1), max(0, y1)
        x2, y2 = min(w, x2), min(h, y2)

        crop = frame[y1:y2, x1:x2]
        if crop.size == 0:
            return None
        return crop

    def _recognize_text(self, crop) -> tuple[str | None, float]:
        if crop is None or crop.size == 0:
            return None, 0.0

        gray = cv2.cvtColor(crop, cv2.COLOR_BGR2GRAY)
        if gray.shape[0] < 50:
            gray = cv2.resize(
                gray, (0, 0), fx=2.0, fy=2.0, interpolation=cv2.INTER_CUBIC
            )

        results = self.reader.readtext(gray)
        if not results:
            return None, 0.0

        results = sorted(results, key=lambda r: r[0][0][0])
        full_text, total_conf = "", 0.0
        for _, text, conf in results:
            full_text += "".join(c for c in text if c.isalnum()).upper()
            total_conf += conf

        avg_conf = total_conf / len(results)
        return full_text.strip() or None, avg_conf
