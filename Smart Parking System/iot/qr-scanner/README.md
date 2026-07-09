# Standalone QR Scanner

A standalone Python application that scans QR codes using a webcam (or from mock strings) and directly hits the Smart Parking System Backend API.

## Setup

```bash
cd iot/qr-scanner
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt
```

*(Note: On Linux, `pyzbar` may require installing `zbar` via system package manager: `sudo apt-get install libzbar0`)*

## Configuration

You can configure the scanner using Environment Variables:

| Variable | Default | Description |
|---|---|---|
| `BACKEND_URL` | `http://localhost:8080` | URL of the Spring Boot Backend |
| `GATE_ID` | `gate1` | Gate ID for this scanner location |
| `CAMERA_SOURCE` | `0` | Default webcam index (0) |

## Usage

### 1. Camera Mode
Run the script to open the webcam and scan physical QR codes shown to the lens:
```bash
python scanner.py
```

### 2. Mock Mode (CLI)
Skip the camera and inject a fake QR code directly into the backend for testing:
```bash
python scanner.py --mock "BOOKING_12345"
```
