"""
ANPR Camera Mock — Simulates an IoT edge camera that:
  1. Reads a vehicle image from disk
  2. Detects and recognizes the license plate (on-device, edge AI)
  3. Publishes the plate text to MQTT broker

Usage:
  # Process a single image:
  python camera.py --image path/to/car.jpg

  # Watch a folder and process each new image file that appears:
  python camera.py --watch path/to/folder/

  # Skip the real AI model and inject a plate directly (fastest for demos):
  python camera.py --mock-plate "30F12345"
"""

import argparse
import cv2
import logging
import os
import sys
import time

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    handlers=[logging.StreamHandler(sys.stdout)],
)
logger = logging.getLogger("ANPRCamera")

from config import HF_MODEL_REPO, HF_MODEL_FILE, HF_CACHE_DIR, OCR_CONFIDENCE_THRESHOLD, MIN_PLATE_LENGTH, WATCH_INTERVAL
from mqtt_publisher import MQTTPublisher


def process_image(path: str, anpr, publisher: MQTTPublisher) -> bool:
    logger.info(f"Processing image: {path}")
    frame = cv2.imread(path)
    if frame is None:
        logger.error(f"Cannot read image: {path}")
        return False

    plate_text, confidence = anpr.detect_and_recognize(frame)

    if plate_text and len(plate_text) >= MIN_PLATE_LENGTH and confidence >= OCR_CONFIDENCE_THRESHOLD:
        logger.info(f"Plate detected: '{plate_text}' (confidence={confidence:.2f})")
        publisher.publish_plate(plate_text, confidence)
        return True
    else:
        logger.warning(
            f"No valid plate found. "
            f"text='{plate_text}' confidence={confidence:.2f} "
            f"min_length={MIN_PLATE_LENGTH} min_confidence={OCR_CONFIDENCE_THRESHOLD}"
        )
        return False


def run_single(image_path: str, publisher: MQTTPublisher):
    from anpr import ANPREngine
    anpr = ANPREngine(HF_MODEL_REPO, HF_MODEL_FILE, HF_CACHE_DIR)
    process_image(image_path, anpr, publisher)


def run_watch(folder: str, publisher: MQTTPublisher):
    from anpr import ANPREngine
    anpr = ANPREngine(HF_MODEL_REPO, HF_MODEL_FILE, HF_CACHE_DIR)

    logger.info(f"Watching folder '{folder}' for new images every {WATCH_INTERVAL}s...")
    seen = set()

    while True:
        try:
            entries = {
                f for f in os.listdir(folder)
                if f.lower().endswith((".jpg", ".jpeg", ".png", ".bmp"))
            }
            new_files = entries - seen
            for filename in sorted(new_files):
                full_path = os.path.join(folder, filename)
                process_image(full_path, anpr, publisher)
                seen.add(filename)
            time.sleep(WATCH_INTERVAL)
        except KeyboardInterrupt:
            logger.info("Watch mode stopped.")
            break


def run_mock(plate_text: str, publisher: MQTTPublisher):
    logger.info(f"[MOCK MODE] Injecting plate directly: '{plate_text}'")
    publisher.publish_plate(plate_text)


def run_api(publisher: MQTTPublisher):
    from fastapi import FastAPI, UploadFile, File, HTTPException, Request
    from fastapi.staticfiles import StaticFiles
    import uvicorn
    from anpr import ANPREngine
    import numpy as np
    import datetime
    
    app = FastAPI(title="ANPR Camera API")
    
    # Ensure local directory exists
    os.makedirs("data/images", exist_ok=True)
    app.mount("/images", StaticFiles(directory="data/images"), name="images")
    
    anpr = ANPREngine(HF_MODEL_REPO, HF_MODEL_FILE, HF_CACHE_DIR)
    
    @app.post("/upload")
    async def upload_image(request: Request, file: UploadFile = File(...)):
        if not file.content_type.startswith("image/"):
            raise HTTPException(status_code=400, detail="File must be an image")
            
        contents = await file.read()
        nparr = np.frombuffer(contents, np.uint8)
        frame = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        
        if frame is None:
            raise HTTPException(status_code=400, detail="Invalid image data")
            
        plate_text, confidence = anpr.detect_and_recognize(frame)
        
        if plate_text and len(plate_text) >= MIN_PLATE_LENGTH and confidence >= OCR_CONFIDENCE_THRESHOLD:
            # Save the file locally
            timestamp_str = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
            safe_filename = f"{timestamp_str}_{file.filename}"
            save_path = os.path.join("data/images", safe_filename)
            with open(save_path, "wb") as f:
                f.write(contents)
                
            image_url = f"{str(request.base_url).rstrip('/')}/images/{safe_filename}"
            
            logger.info(f"API Plate detected: '{plate_text}' (confidence={confidence:.2f}) -> Saved to {save_path}")
            publisher.publish_plate(plate_text, confidence, image_url)
            return {"success": True, "plate": plate_text, "confidence": confidence, "image_url": image_url}
        else:
            logger.warning("API No valid plate found.")
            return {"success": False, "plate": plate_text, "confidence": confidence, "message": "No valid plate found"}
            
    logger.info("Starting API server on 0.0.0.0:8000...")
    uvicorn.run(app, host="0.0.0.0", port=8000)


def main():
    parser = argparse.ArgumentParser(
        description="ANPR Camera Mock — Edge AI license plate reader that publishes to MQTT"
    )
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--image", type=str, metavar="PATH", help="Process a single image file")
    group.add_argument("--watch", type=str, metavar="FOLDER", help="Watch a folder for new images")
    group.add_argument("--mock-plate", type=str, metavar="PLATE", help="Skip AI and inject a plate directly")
    group.add_argument("--api", action="store_true", help="Start FastAPI server to accept image uploads")
    args = parser.parse_args()

    publisher = MQTTPublisher()
    publisher.connect()
    time.sleep(1.0)

    try:
        if args.image:
            if not os.path.isfile(args.image):
                logger.error(f"File not found: {args.image}")
                sys.exit(1)
            run_single(args.image, publisher)

        elif args.watch:
            if not os.path.isdir(args.watch):
                logger.error(f"Folder not found: {args.watch}")
                sys.exit(1)
            run_watch(args.watch, publisher)

        elif args.mock_plate:
            run_mock(args.mock_plate, publisher)

        elif args.api:
            run_api(publisher)

    finally:
        time.sleep(0.5)
        publisher.disconnect()


if __name__ == "__main__":
    main()
