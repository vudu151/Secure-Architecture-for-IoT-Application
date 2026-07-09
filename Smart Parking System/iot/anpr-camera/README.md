# ANPR Camera Mock

A mock IoT edge camera that reads vehicle images, detects license plates using YOLOv8 + EasyOCR, and publishes the result to the MQTT broker.

## Docker Installation & API Mode (Recommended)

The easiest way to run the ANPR camera is via the provided Docker container. This spins up a FastAPI server that you can send images to.

### 1. Build and Run the Container

Make sure you have Docker installed and your MQTT broker (Mosquitto) is running on the `smartparking-backend` network.

```bash
cd iot/anpr-camera

# Build the Docker image
docker build -t anpr-camera-api .

# Run the container
docker run -d \
  --name camera \
  -p 8000:8000 \
  -v "$(pwd)/../../docker/certs:/docker/certs" \
  -v "$(pwd)/models/ocr:/app/models/ocr" \
  -v "$(pwd)/models/plate_detector:/app/models/plate_detector" \
  -e MQTT_HOST=mosquitto \
  --network smartparking-backend \
  anpr-camera-api
```

*Note: On first startup, the camera will automatically download the required YOLOv8 plate detector and EasyOCR models. The `-v` volume mounts ensure these models are cached locally and don't need to be redownloaded on container restarts.*

### 2. Test the API

Once the container is running and the models are downloaded/initialized, you can test the pipeline by uploading an image using `curl`:

```bash
curl -X POST -F "file=@path/to/your/car_image.jpg" http://localhost:8000/upload
```

If successful, the API returns the detected plate and publishes it to the MQTT broker!

---

## Local CLI Usage

If you prefer to run the scripts locally without Docker:

### Setup
```bash
cd iot/anpr-camera
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt
```

### 1. Single Image Mode
Process one image and send the result to MQTT:
```bash
python camera.py --image path/to/car.jpg
```

### 2. Watch Folder Mode
Drop images into a folder and the camera will automatically detect and publish whenever a new file appears:
```bash
python camera.py --watch path/to/images/
```

### 3. Mock Plate Mode (Fastest for demos — no AI model needed)
Skip the AI entirely and inject a plate string directly to MQTT:
```bash
python camera.py --mock-plate "30F12345"
```

## MQTT Output

The camera publishes to topic: `parking/gates/{GATE_ID}/event`

Payload format:
```json
{
  "event": "PLATE_DETECTED",
  "plate": "30F12345"
}
```

## Configuration

All settings can be overridden via environment variables:

| Variable | Default | Description |
|---|---|---|
| `MQTT_HOST` | `localhost` | MQTT broker address |
| `MQTT_PORT` | `8883` | MQTT broker port (mTLS) |
| `GATE_ID` | `gate1` | Gate ID used in MQTT topic |
| `DEVICE_UID` | `anpr_camera_1` | MQTT client ID |
| `HF_MODEL_REPO` | `Koushim/yolov8-license-plate-detection` | HuggingFace Repo for YOLO model |
| `HF_MODEL_FILE` | `best.pt` | Filename of the model inside HF repo |
| `HF_CACHE_DIR` | `models/plate_detector` | Directory to cache downloaded YOLO models |
| `OCR_CONFIDENCE_THRESHOLD` | `0.5` | Min OCR confidence to accept |
| `MIN_PLATE_LENGTH` | `5` | Min characters to be a valid plate |
| `WATCH_INTERVAL` | `2.0` | Folder polling interval (seconds) |
