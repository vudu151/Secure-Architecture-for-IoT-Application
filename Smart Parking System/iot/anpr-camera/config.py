import os
from dotenv import load_dotenv

BASE_DIR = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
env_path = os.path.join(BASE_DIR, ".env")
if os.path.exists(env_path):
    load_dotenv(env_path)

# --- MQTT mTLS Configuration ---
MQTT_HOST = os.environ.get("MQTT_HOST", "localhost")
MQTT_PORT = int(os.environ.get("MQTT_PORT", 8883))
MQTT_USE_TLS = os.environ.get("MQTT_USE_TLS", "True").lower() in ("true", "1", "yes")

CA_CERT = os.environ.get(
    "CA_CERT", os.path.join(BASE_DIR, "docker", "certs", "ca", "ca.crt")
)
CLIENT_CERT = os.environ.get(
    "CLIENT_CERT",
    os.path.join(BASE_DIR, "docker", "certs", "clients", "rpi_gate1", "rpi_gate1.crt"),
)
CLIENT_KEY = os.environ.get(
    "CLIENT_KEY",
    os.path.join(BASE_DIR, "docker", "certs", "clients", "rpi_gate1", "rpi_gate1.key"),
)

# --- Device Identity ---
GATE_ID = os.environ.get("GATE_ID", "gate1")
DEVICE_UID = os.environ.get("DEVICE_UID", "anpr_camera_1")

# --- MQTT Topics ---
EVENT_TOPIC = f"parking/gates/{GATE_ID}/event"

# --- ANPR Settings ---
HF_MODEL_REPO = os.environ.get("HF_MODEL_REPO", "Koushim/yolov8-license-plate-detection")
HF_MODEL_FILE = os.environ.get("HF_MODEL_FILE", "best.pt")
HF_CACHE_DIR = os.environ.get(
    "HF_CACHE_DIR",
    os.path.join(os.path.dirname(__file__), "models", "plate_detector"),
)
OCR_CONFIDENCE_THRESHOLD = float(os.environ.get("OCR_CONFIDENCE_THRESHOLD", 0.5))
MIN_PLATE_LENGTH = int(os.environ.get("MIN_PLATE_LENGTH", 5))

# --- Image Watch Mode Settings ---
# If set, the camera will watch this folder for new images and process them automatically
WATCH_FOLDER = os.environ.get("WATCH_FOLDER", None)
# How often to poll the folder for new images (seconds)
WATCH_INTERVAL = float(os.environ.get("WATCH_INTERVAL", 2.0))

# --- Stream Settings ---
CAMERA_SOURCE = os.environ.get("CAMERA_SOURCE", None)
