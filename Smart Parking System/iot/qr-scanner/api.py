from fastapi import FastAPI, UploadFile, File, Form, HTTPException
from typing import Optional
import requests
import os
import logging
import sys
import io
from PIL import Image
from pyzbar.pyzbar import decode

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] QRScannerAPI: %(message)s",
    handlers=[logging.StreamHandler(sys.stdout)],
)
logger = logging.getLogger("QRScannerAPI")

app = FastAPI(title="QR Scanner Mock API")

SERVER_URL = os.environ.get("SERVER_URL", "http://server:8080")
CAMERA_URL = os.environ.get("CAMERA_URL", "http://camera:8000")
GATE_ID = os.environ.get("GATE_ID", "gate1")

@app.post("/scan")
async def process_scan(
    camera_image: Optional[UploadFile] = File(default=None),
    qr_data: Optional[str] = Form(default=None),
    qr_image: Optional[UploadFile] = File(default=None)
):
    """
    Receives QR string/image and optional car image.
    1. Forwards QR string to Backend Server.
    2. Forwards car image to ANPR Camera (or triggers capture).
    """
    if not qr_data and not qr_image:
        raise HTTPException(status_code=400, detail="Must provide either qr_data or qr_image")

    if qr_image:
        try:
            img_bytes = await qr_image.read()
            img = Image.open(io.BytesIO(img_bytes))
            decoded_objects = decode(img)
            if not decoded_objects:
                raise HTTPException(status_code=400, detail="Could not decode QR code from the provided image")
            qr_data = decoded_objects[0].data.decode("utf-8")
            logger.info(f"Decoded QR code from image: {qr_data}")
        except Exception as e:
            logger.error(f"Failed to process QR image: {e}")
            raise HTTPException(status_code=400, detail="Invalid image file or decoding failed")
    
    logger.info(f"Processing scan request for QR: {qr_data}")
    
    # 1. Forward QR code to Server
    server_endpoint = f"{SERVER_URL}/api/v1/devices/verify-qr"
    payload = {
        "gate_id": GATE_ID,
        "qr_code_data": qr_data
    }
    
    try:
        logger.info(f"Forwarding QR data to server: {server_endpoint}")
        server_res = requests.post(server_endpoint, json=payload, timeout=5.0)
        server_res.raise_for_status()
        logger.info("Server verified QR successfully.")
    except Exception as e:
        logger.error(f"Failed to verify QR with server (mock mode will continue anyway): {e}")
        # We don't raise an error here because we want to mock the flow even without a backend.
        
    # 2. Forward Image to Camera (or trigger capture)
    if camera_image:
        camera_endpoint = f"{CAMERA_URL}/upload"
        try:
            logger.info(f"Forwarding image {camera_image.filename} to ANPR camera: {camera_endpoint}")
            file_bytes = await camera_image.read()
            files = {'file': (camera_image.filename, file_bytes, camera_image.content_type)}
            camera_res = requests.post(camera_endpoint, files=files, timeout=10.0)
            camera_res.raise_for_status()
            logger.info(f"Camera processed image. Response: {camera_res.json()}")
        except Exception as e:
            logger.error(f"Failed to forward image to camera: {e}")
            raise HTTPException(status_code=502, detail="Failed to contact Camera")
    else:
        camera_endpoint = f"{CAMERA_URL}/capture"
        try:
            logger.info(f"Triggering camera capture: {camera_endpoint}")
            camera_res = requests.post(camera_endpoint, timeout=15.0)
            camera_res.raise_for_status()
            logger.info(f"Camera processed capture. Response: {camera_res.json()}")
        except Exception as e:
            logger.error(f"Failed to trigger camera capture: {e}")
            raise HTTPException(status_code=502, detail="Failed to trigger Camera capture")
        
    return {"status": "success", "message": "QR verified and image processed by camera."}
