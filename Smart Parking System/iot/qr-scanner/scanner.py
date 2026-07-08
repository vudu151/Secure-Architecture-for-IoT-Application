import cv2
from pyzbar import pyzbar
import requests
import time
import argparse
import logging
import sys
import os

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] QRScanner: %(message)s",
    handlers=[logging.StreamHandler(sys.stdout)],
)
logger = logging.getLogger("StandaloneQR")

BACKEND_URL = os.environ.get("BACKEND_URL", "http://localhost:8080")
GATE_ID = os.environ.get("GATE_ID", "gate1")

def send_to_backend(qr_data: str) -> bool:
    """Sends the decoded QR data to the smart parking backend."""
    url = f"{BACKEND_URL}/api/v1/devices/verify-qr"
    payload = {
        "gate_id": GATE_ID,
        "qr_code_data": qr_data
    }
    
    logger.info(f"Sending QR Code '{qr_data}' to backend API...")
    try:
        response = requests.post(url, json=payload, timeout=5.0)
        if response.status_code in [200, 201]:
            logger.info("✅ SUCCESS: Backend verified QR code successfully!")
            return True
        else:
            logger.warning(f"❌ FAILED: Backend rejected QR code. Status: {response.status_code} - {response.text}")
            return False
    except requests.exceptions.ConnectionError:
        logger.error(f"❌ ERROR: Could not connect to backend at {url}.")
        return False
    except Exception as e:
        logger.error(f"❌ ERROR: Failed to communicate with backend: {e}")
        return False

def run_camera():
    camera_source = os.environ.get("CAMERA_SOURCE", "0")
    try:
        camera_source = int(camera_source)
    except ValueError:
        pass # allow string paths for video files

    logger.info(f"Opening camera source: {camera_source}")
    cap = cv2.VideoCapture(camera_source)
    
    if not cap.isOpened():
        logger.error("Could not open video source.")
        sys.exit(1)

    cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
    cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)

    logger.info("Starting scanner loop. Show a QR code to the camera. Press 'q' to exit.")
    
    cooldown_period = 5.0
    last_scan_time = 0
    last_qr_data = None
    
    try:
        while True:
            ret, frame = cap.read()
            if not ret:
                time.sleep(0.1)
                continue

            current_time = time.time()
            is_in_cooldown = (current_time - last_scan_time) < cooldown_period
            
            # Display status on frame
            status_text = "SCANNING..."
            color = (0, 255, 0)
            if is_in_cooldown:
                status_text = f"COOLDOWN ({cooldown_period - (current_time - last_scan_time):.1f}s)"
                color = (0, 0, 255)
            
            cv2.putText(frame, status_text, (10, 30), cv2.FONT_HERSHEY_SIMPLEX, 1, color, 2)
            
            if not is_in_cooldown:
                decoded_objects = pyzbar.decode(frame)
                for obj in decoded_objects:
                    if obj.type == 'QRCODE':
                        qr_data = obj.data.decode('utf-8')
                        
                        # Draw box around QR
                        pts = obj.polygon
                        if len(pts) == 4:
                            pts = [ (p.x, p.y) for p in pts ]
                            cv2.polylines(frame, [__import__("numpy").array(pts)], True, (255, 0, 0), 3)
                        
                        logger.info(f"QR Code detected: {qr_data}")
                        
                        if qr_data != last_qr_data or not is_in_cooldown:
                            send_to_backend(qr_data)
                            last_scan_time = time.time()
                            last_qr_data = qr_data
                        break # Only process one QR per frame

            cv2.imshow("Standalone QR Scanner", frame)
            
            if cv2.waitKey(1) & 0xFF == ord('q'):
                break
                
    except KeyboardInterrupt:
        logger.info("Interrupted by user.")
    finally:
        cap.release()
        cv2.destroyAllWindows()
        logger.info("Scanner stopped.")

def main():
    parser = argparse.ArgumentParser(description="Standalone QR Scanner for Smart Parking System")
    parser.add_argument("--mock", type=str, help="Mock a QR code string without opening the camera")
    args = parser.parse_args()

    if args.mock:
        logger.info(f"Running in MOCK mode with QR string: {args.mock}")
        send_to_backend(args.mock)
    else:
        run_camera()

if __name__ == "__main__":
    main()
