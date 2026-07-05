# Secure IoT Smart Parking System (Monorepo)

Hệ thống quản lý bãi đỗ xe thông minh an toàn, tích hợp thiết bị IoT (ESP32 cảm biến, Raspberry Pi điều khiển cổng ANPR) cùng ứng dụng di động cho tài xế và trang quản trị trung tâm.

## 🚀 Kiến trúc Dự án Phân lớp

Dự án được tổ chức dưới dạng Monorepo bao gồm các thành phần sau:
*   **/backend**: API Services viết bằng **Spring Boot 3.2.x**, kết nối PostgreSQL và cache Redis.
*   **/web**: Trang Dashboard quản trị cho Admin viết bằng **ReactJS (Vite)** & **Ant Design 5**.
*   **/mobile**: Ứng dụng di động của tài xế viết bằng **Flutter**, tích hợp bản đồ 2D và ví điện tử.
*   **/iot/esp32**: Firmware cho vi điều khiển ESP32 đọc cảm biến HC-SR04 gửi trạng thái ô đỗ qua MQTT.
*   **/iot/raspberry-pi**: Tập lệnh Python nhận dạng biển số xe (YOLOv8 ANPR), quét QR và điều khiển barie cổng.
*   **/docker**: Hạ tầng triển khai container hóa:
    *   `docker-compose.yml`: Kịch bản điều phối các service.
    *   `docker/certs`: Tập lệnh tự động sinh CA nội bộ và chứng chỉ số mTLS cho thiết bị ngoại vi.
    *   `docker/mosquitto`: Cấu hình Broker MQTT hỗ trợ mTLS cổng 8883 và phân quyền ACL.
    *   `docker/nginx`: Cấu hình Gateway API, SSL Termination, giới hạn tần suất (Rate Limiting).
    *   `docker/postgres`: Kịch bản khởi tạo lược đồ cơ sở dữ liệu (`init.sql`).
*   **/docs**: Tài liệu báo cáo chi tiết đề tài (`Bao_cao_Do_an_Smart_Parking.docx`).

---

## 🔒 Các Giải pháp Bảo mật Triển khai

1.  **Mutual TLS (mTLS) Auth**: Mọi thiết bị IoT kết nối đến MQTT Broker qua cổng 8883 bắt buộc phải trình chứng chỉ số X.509 hợp lệ được ký bởi CA nội bộ của hệ thống.
2.  **Mã hóa Dữ liệu AES-256**: Biển số xe của người dùng được mã hóa đối xứng ở chế độ AES/CBC/PKCS5Padding trước khi lưu xuống cơ sở dữ liệu để bảo vệ dữ liệu nhạy cảm.
3.  **Chống Tấn công Phát lại (Anti-Replay)**: Lệnh điều khiển barrier đính kèm mã ngẫu nhiên UUID Nonce và Timestamp. Hệ thống sử dụng Redis để lưu và đối soát Nonce trong cửa sổ thời gian 5 giây nhằm ngăn chặn bắt gói tin gửi lại.
4.  **JWT Stateless Blacklisting**: Sử dụng JSON Web Token để xác thực phân quyền REST APIs và WebSocket. Hệ thống lưu token đăng xuất vào Redis Blacklist cho đến khi hết hạn để thực hiện Secure Logout.
5.  **Security Audit Logging**: Mọi hành vi đăng nhập, điều khiển thiết bị, kích hoạt/khóa tài khoản đều được ghi nhận tự động vào bảng nhật ký kiểm toán kèm thông tin IP thực của Client.

---

## ⚙️ Hướng dẫn Cài đặt & Chạy Hệ thống

### 1. Chuẩn bị Môi trường
Yêu cầu máy cài đặt sẵn: Docker, Docker Compose, Java 17, Node.js, Flutter SDK và Python 3.

### 2. Sinh chứng chỉ số mTLS
Trước khi khởi động các container Docker, chúng ta cần sinh hệ thống chứng chỉ số bảo mật cho Mosquitto Broker và các client:

```bash
cd docker/certs
chmod +x generate_certs.sh
./generate_certs.sh
```

Lệnh trên sẽ tạo ra thư mục `ca`, `broker`, và các thư mục chứng chỉ riêng cho từng client (esp32_slot_a01...a10, esp32_slot_b01...b10, rpi_gate1, rpi_gate2, backend) trong `/docker/certs/clients/`.

### 3. Cấu hình biến môi trường
Tạo file `.env` từ file mẫu `.env.example` ở thư mục gốc và điền các khóa bí mật của bạn:

```bash
cp .env.example .env
```

### 4. Khởi động hạ tầng Docker
Khởi chạy toàn bộ hệ thống (PostgreSQL, Redis, Mosquitto, Nginx và Spring Boot Backend):

```bash
docker compose up -d
```

Hệ thống sẽ tự động khởi tạo cơ sở dữ liệu bằng file `docker/postgres/init.sql`, Nginx sẽ lắng nghe tại cổng `80` (chuyển hướng sang `443` HTTPS), và Mosquitto chạy cổng `1883` (nội bộ) và `8883` (mTLS ngoại vi).

### 5. Chạy Web Admin Dashboard
Di chuyển vào thư mục `web`, cài đặt dependencies và chạy dev server:

```bash
cd web
npm install
npm run dev
```

Truy cập Dashboard qua HTTPS: `https://localhost` (hoặc `http://localhost:5173` trong chế độ phát triển).
*   Tài khoản Admin mặc định: `admin@smartparking.vn` / `admin123`

### 6. Chạy Flutter Mobile App
Yêu cầu mở trình giả lập Android hoặc cắm thiết bị thật. Di chuyển vào thư mục `mobile` và chạy:

```bash
cd mobile
flutter pub get
flutter run
```

*   Tài khoản Driver thử nghiệm: `driver01@example.com` / `driver123`

---

## 📖 Tài liệu Báo cáo Đồ án

Tài liệu báo cáo chi tiết theo chuẩn Đại học Bách Khoa Hà Nội được lưu trữ tại:
*   [docs/Bao_cao_Do_an_Smart_Parking.docx](file:///home/duvx/Documents/Hust%20Master/Secure-Architecture-for-IoT-Application/Smart%20Parking%20System/docs/Bao_cao_Do_an_Smart_Parking.docx)

Để cập nhật nội dung tài liệu tự động dựa trên mã nguồn hiện tại, hãy chạy script Python:
```bash
python3 docs/generate_report.py
```
