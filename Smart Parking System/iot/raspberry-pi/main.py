import cv2
import time
import logging
import sys
import signal
import os
import threading
from config import CAMERA_SOURCE, MODEL_PATH, GATE_ID
from anpr import ANPREngine
from qr_scanner import QRScanner
from gate_controller import GateController
from mqtt_handler import MQTTHandler

# Configure Logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] (%(threadName)s) %(name)s: %(message)s",
    handlers=[
        logging.StreamHandler(sys.stdout)
    ]
)
logger = logging.getLogger("Main")

class SmartParkingGateApp:
    def __init__(self):
        self.running = True
        
        # 1. Initialize Gate Controller
        self.gate_controller = GateController(gate_id=GATE_ID)
        
        # 2. Initialize MQTT Handler
        self.mqtt_handler = MQTTHandler(self.gate_controller)
        
        # 3. Initialize QR Scanner
        self.qr_scanner = QRScanner()
        
        # 4. Initialize ANPR Engine (handling model missing gracefully)
        self.anpr_engine = None
        if os.path.exists(MODEL_PATH):
            try:
                self.anpr_engine = ANPREngine(MODEL_PATH)
            except Exception as e:
                logger.error(f"Error loading ANPR engine: {e}. Running in QR-only mode.")
        else:
            logger.warning(f"ONNX Model not found at '{MODEL_PATH}'. Running in QR-only mode.")
            logger.warning("Please download the YOLOv8 license plate model to enable license plate recognition.")

        # 5. Cooldown settings to prevent continuous trigger
        self.last_detection_time = 0
        self.cooldown_period = 10.0 # 10 seconds cooldown after successful trigger
        
        # Track last detected plate & QR to avoid spamming
        self.last_detected_value = None
        self.detected_value_expire = 0

    def start(self):
        # Connect to MQTT Broker
        self.mqtt_handler.connect()
        
        # Start Video Capture Loop
        self.capture_loop()

    def capture_loop(self):
        logger.info(f"Opening camera source: {CAMERA_SOURCE}")
        cap = cv2.VideoCapture(CAMERA_SOURCE)
        if not cap.isOpened():
            logger.error("Could not open video source. Generating simulated frames for testing...")
            # We will use a simulation loop if camera is unavailable
            self.simulated_capture_loop()
            return

        # Set lower resolution to optimize CPU on PC/Laptop
        cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
        cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)

        show_gui = True
        logger.info("Starting main capture loop. Press 'q' in the window or Ctrl+C in terminal to exit.")
        
        frame_count = 0
        while self.running:
            ret, frame = cap.read()
            if not ret:
                logger.error("Failed to grab frame.")
                time.sleep(0.1)
                continue

            frame_count += 1
            current_time = time.time()
            is_in_cooldown = (current_time - self.last_detection_time) < self.cooldown_period
            
            # Draw status overlay on the frame
            gate_open = self.gate_controller.is_open()
            self.draw_overlay(frame, gate_open, is_in_cooldown)

            # Process frame (skip heavy processing if in cooldown)
            if not is_in_cooldown:
                # 1. Scan for QR code (high priority, fast execution)
                qr_data = self.qr_scanner.scan_from_frame(frame)
                if qr_data:
                    logger.info(f"Found QR code: {qr_data}. Publishing event...")
                    self.mqtt_handler.publish_event("QR_SCANNED", qr_data)
                    self.last_detection_time = current_time
                    self.last_detected_value = qr_data
                    self.detected_value_expire = current_time + self.cooldown_period

                # 2. Scan for License Plate (only if ANPR engine is available and not in cooldown)
                elif self.anpr_engine and frame_count % 5 == 0: # Process every 5th frame to save CPU
                    cropped_plate, bbox = self.anpr_engine.detect_plate(frame)
                    if cropped_plate is not None:
                        plate_text, conf = self.anpr_engine.recognize_text(cropped_plate)
                        if plate_text and len(plate_text) >= 5: # Valid plate length
                            logger.info(f"Detected Plate: {plate_text} (Conf: {conf:.2f})")
                            
                            # Draw bounding box around plate
                            x, y, w, h = bbox
                            cv2.rectangle(frame, (x, y), (x+w, y+h), (0, 255, 0), 2)
                            cv2.putText(frame, f"{plate_text} ({conf:.2f})", (x, y-10),
                                        cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 255, 0), 2)
                            
                            logger.info(f"Publishing plate event: {plate_text}...")
                            self.mqtt_handler.publish_event("PLATE_DETECTED", plate_text)
                            self.last_detection_time = current_time
                            self.last_detected_value = plate_text
                            self.detected_value_expire = current_time + self.cooldown_period
            else:
                # In cooldown, overlay detection info
                if self.last_detected_value and current_time < self.detected_value_expire:
                    cv2.putText(frame, f"PROCESSED: {self.last_detected_value}", (20, 100),
                                cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 255), 2)
                    cv2.putText(frame, f"Cooldown: {int(self.detected_value_expire - current_time)}s", (20, 130),
                                cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 255, 255), 1)

            # Display frame
            if show_gui:
                try:
                    cv2.imshow("Smart Parking - Gate Controller Simulation", frame)
                    key = cv2.waitKey(1) & 0xFF
                    if key == ord('q'):
                        self.running = False
                except Exception as e:
                    logger.warning(f"Could not display GUI window: {e}. Running headless mode.")
                    show_gui = False

            # Idle CPU slightly
            time.sleep(0.01)

        cap.release()
        cv2.destroyAllWindows()
        logger.info("Webcam capture loop terminated.")

    def simulated_capture_loop(self):
        """
        Runs a simulation loop in case a hardware camera is not available.
        Listens for keystrokes in terminal or simple simulation triggers.
        """
        logger.info("Simulated camera active. In terminal, type:")
        logger.info("  'qr <data>'  - to simulate scanning a QR code")
        logger.info("  'plate <val>'- to simulate detecting a license plate")
        logger.info("  'exit'       - to quit")
        
        def console_input_thread():
            while self.running:
                try:
                    cmd_line = input().strip()
                    if not cmd_line:
                        continue
                    
                    parts = cmd_line.split(" ", 1)
                    cmd = parts[0].lower()
                    
                    if cmd == "exit":
                        self.running = False
                        break
                    
                    current_time = time.time()
                    is_in_cooldown = (current_time - self.last_detection_time) < self.cooldown_period
                    if is_in_cooldown:
                        logger.warning(f"System is in cooldown. Please wait {int(self.cooldown_period - (current_time - self.last_detection_time))}s.")
                        continue
                    
                    if cmd == "qr" and len(parts) > 1:
                        qr_data = parts[1]
                        logger.info(f"[SIMULATED] QR code scanned: {qr_data}")
                        self.mqtt_handler.publish_event("QR_SCANNED", qr_data)
                        self.last_detection_time = current_time
                    elif cmd == "plate" and len(parts) > 1:
                        plate_text = parts[1]
                        logger.info(f"[SIMULATED] Plate detected: {plate_text}")
                        self.mqtt_handler.publish_event("PLATE_DETECTED", plate_text)
                        self.last_detection_time = current_time
                    else:
                        logger.warning(f"Unknown simulation command. E.g. 'qr my_booking_code' or 'plate 30F12345'")
                except (KeyboardInterrupt, EOFError):
                    self.running = False
                    break

        threading.Thread(target=console_input_thread, daemon=True).start()
        
        while self.running:
            time.sleep(1)

    def draw_overlay(self, frame, gate_open, is_in_cooldown):
        """
        Draws state dashboard directly onto the webcam frame.
        """
        # Status Bar Background
        cv2.rectangle(frame, (10, 10), (320, 80), (50, 50, 50), -1)
        cv2.rectangle(frame, (10, 10), (320, 80), (200, 200, 200), 1)

        # Gate status
        status_text = "GATE: OPEN" if gate_open else "GATE: CLOSED"
        status_color = (0, 255, 0) if gate_open else (0, 0, 255) # Green vs Red
        cv2.putText(frame, status_text, (20, 35), cv2.FONT_HERSHEY_SIMPLEX, 0.7, status_color, 2)

        # System Mode status
        mode_text = "ANPR+QR Mode" if self.anpr_engine else "QR-Only Mode"
        cv2.putText(frame, mode_text, (20, 55), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255, 255, 255), 1)

        # Cooldown state indicator
        if is_in_cooldown:
            cv2.circle(frame, (300, 30), 8, (0, 165, 255), -1) # Orange circle
            cv2.putText(frame, "HOLD", (280, 55), cv2.FONT_HERSHEY_SIMPLEX, 0.4, (0, 165, 255), 1)
        else:
            cv2.circle(frame, (300, 30), 8, (0, 255, 0), -1) # Green circle
            cv2.putText(frame, "READY", (280, 55), cv2.FONT_HERSHEY_SIMPLEX, 0.4, (0, 255, 0), 1)

    def shutdown(self):
        logger.info("Shutting down Gate Application...")
        self.running = False
        self.mqtt_handler.disconnect()
        self.gate_controller.cleanup()
        logger.info("Shutdown complete.")

def main():
    app = SmartParkingGateApp()

    def signal_handler(sig, frame):
        logger.info("Signal received. Initiating shutdown...")
        app.shutdown()
        sys.exit(0)

    signal.signal(signal.SIGINT, signal_handler)
    signal.signal(signal.SIGTERM, signal_handler)

    logger.info("Starting Smart Parking Gate Application...")
    app.start()

if __name__ == "__main__":
    main()
