FROM python:3.11-slim

# Install system dependencies required for OpenCV
RUN apt-get update && apt-get install -y \
    libgl1 \
    libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Install python dependencies
COPY requirements.txt .
# Install CPU-only PyTorch to avoid massive NVIDIA GPU libraries
RUN pip install --no-cache-dir --default-timeout=1000 torch torchvision --index-url https://download.pytorch.org/whl/cpu

RUN pip install --no-cache-dir --default-timeout=1000 -r requirements.txt

# Copy application files
COPY . /app

# Models are downloaded at runtime into:
#   models/ocr/            — EasyOCR weights     (mount: -v $(pwd)/models/ocr:/app/models/ocr)
#   models/plate_detector/ — YOLOv8 HF weights   (mount: -v $(pwd)/models/plate_detector:/app/models/plate_detector)

# Expose the API port
EXPOSE 8000

# Run the API server
CMD ["python", "camera.py", "--api"]
