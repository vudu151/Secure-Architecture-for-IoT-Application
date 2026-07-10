FROM python:3.9-slim

WORKDIR /app

# Cài đặt các thư viện hệ thống cần thiết cho OpenCV và ZBar (quét QR)
RUN apt-get update && apt-get install -y \
    libzbar0 \
    libgl1 \
    libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

# Cài đặt thư viện Python
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy mã nguồn
COPY . .

# Chạy ứng dụng
CMD ["python", "-u", "main.py"]
