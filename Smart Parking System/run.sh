docker-compose -f docker-compose.mock.yaml up --build -d

curl -X POST \
  -F "qr_image=@/Users/zero/Projects/sdh/iot/btl/images/actual_qr.png" \
  -F "camera_image=@/Users/zero/Projects/sdh/iot/btl/images/image.png" \
  http://localhost:8001/scan

curl -X POST \
  -F "qr_data=1234567890" \
  -F "camera_image=@/Users/zero/Projects/sdh/iot/btl/Secure-Architecture-for-IoT-Application/Smart Parking System/images/car.png" \
  http://localhost:8001/scan