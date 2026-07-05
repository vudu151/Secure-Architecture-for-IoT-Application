import os

# Base directory of the project
BASE_DIR = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# --- MQTT mTLS Configuration ---
MQTT_HOST = "localhost" # Connect locally since we run on the same PC
MQTT_PORT = 8883        # Secure mTLS port

# Certificates paths relative to project root
CA_CERT = os.path.join(BASE_DIR, "docker", "certs", "ca", "ca.crt")
CLIENT_CERT = os.path.join(BASE_DIR, "docker", "certs", "clients", "rpi_gate1", "rpi_gate1.crt")
CLIENT_KEY = os.path.join(BASE_DIR, "docker", "certs", "clients", "rpi_gate1", "rpi_gate1.key")

# --- Gate Configuration ---
GATE_ID = "gate1"
DEVICE_UID = "rpi_gate1"
FIRMWARE_VER = "1.0.0"

# --- Backend Configuration ---
# Point directly to the backend service. 
# Use "http://localhost:8080/api/v1" for direct connection
# or "https://localhost/api/v1" if testing through Nginx gateway (disable SSL validation in requests if using self-signed certs)
API_BASE_URL = "http://localhost:8080/api/v1"

# --- ANPR & Video Source Settings ---
# Use 0 for laptop's built-in webcam, or a video file path (e.g. "test_video.mp4") for simulation
CAMERA_SOURCE = 0 

# Path to the license plate detector YOLOv8 ONNX model
MODEL_PATH = os.path.join(BASE_DIR, "iot", "raspberry-pi", "models", "license_plate_detector.onnx")

# OCR Confidence threshold
OCR_CONFIDENCE_THRESHOLD = 0.5
