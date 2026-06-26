# Báo cáo Kiến trúc & Thiết kế Hệ thống Bãi đỗ xe Thông minh và Đặt chỗ trước (Smart Parking System) - Version 2 (Java & PostgreSQL)

Hệ thống này được thiết kế theo mô hình kiến trúc 3 tầng tiêu chuẩn, tích hợp ứng dụng di động (Mobile App), ứng dụng web quản trị (Web App), phần cứng IoT tự chế và các cơ chế bảo mật nâng cao. Toàn bộ phần Backend xử lý trung tâm được xây dựng trên nền tảng Java (Spring Boot) kết hợp hệ quản trị cơ sở dữ liệu mạnh mẽ PostgreSQL.

---

## 1. Sơ đồ Kiến trúc Tổng quan (System Architecture)

Hệ thống được tổ chức thành 3 tầng chức năng độc lập nhưng liên kết chặt chẽ:

```
+-------------------------------------------------------------------------+
|                        TẦNG ỨNG DỤNG (APPLICATION LAYER)                |
|  +---------------------------+       +-------------------------------+  |
|  |   Flutter Mobile App      |       |       ReactJS Web App         |  |
|  |  (Đặt chỗ, Quét QR, Nav)  |       |   (Quản trị, Analytics)       |  |
|  +--------------+------------+       +---------------+---------------+  |
+-----------------|------------------------------------|------------------+
                  | HTTPS (JWT Auth)                   | HTTPS (JWT Auth)
                  v                                    v
+-------------------------------------------------------------------------+
|                    TẦNG MẠNG & NỀN TẢNG (NETWORK & PLATFORM LAYER)      |
|  +-------------------------------------------------------------------+  |
|  |                       API Gateway (Reverse Proxy)                 |  |
|  +----------------------------------+--------------------------------+  |
|                                     |                                   |
|  +----------------------------------v--------------------------------+  |
|  |         Backend Server: Java Spring Boot (Spring Security)        |  |
|  +--------+-------------------------+--------------------+-----------+  |
|           |                                              |              |
|           v (Spring Data JPA)                            v (Eclipse Paho)
|  +------------------+                              +-----------+        |
|  |    PostgreSQL    |                              | Mosquitto |        |
|  | (Row-level Lock) |                              |  (MQTT)   |        |
|  +------------------+                              +-----+-----+        |
+----------------------------------------------------------|--------------+
                                                           | MQTT over TLS
                                                           | (Port 8883)
                                                           v
+-------------------------------------------------------------------------+
|                        TẦNG THIẾT BỊ (DEVICE LAYER)                     |
|  +-------------------------------------------------------------------+  |
|  |                    Raspberry Pi 4 (Edge Gateway)                  |  |
|  |      - Xử lý nhận diện biển số xe (ANPR với OpenCV/YOLO)          |  |
|  |      - Điều khiển Rào chắn (Servo/Relay)                          |  |
|  +----------------------------------+--------------------------------+  |
|                                     | RS485 / BLE / Wi-Fi               |
|                                     v                                   |
|  +-------------------------------------------------------------------+  |
|  |                     Cụm Thiết bị Cảm biến tại chỗ                 |  |
|  |      - ESP32 Controller                                           |  |
|  |      - Cảm biến khoảng cách Siêu âm (HC-SR04) / Hồng ngoại        |  |
|  +-------------------------------------------------------------------+  |
+-------------------------------------------------------------------------+
```

---

## 2. Mô tả Thành phần & Công nghệ Sử dụng

### 2.1 Tầng Thiết bị (Device Layer)
* **ESP32 Nodes (Tại mỗi vị trí đỗ):**
    * *Nhiệm vụ:* Đọc dữ liệu từ cảm biến siêu âm (HC-SR04) theo chu kỳ định sẵn nhằm xác định sự hiện diện của phương tiện tại ô đỗ.
    * *Lý do chọn:* Chip xử lý lõi kép 32-bit hiệu năng cao, tích hợp Wi-Fi/BLE, hỗ trợ phần cứng tăng tốc mã hóa (AES, SHA-2, RSA), tối ưu cho việc bảo mật thiết bị đầu cuối.
* **Raspberry Pi 4 (Edge Gateway tại Cổng ra/vào):**
    * *Nhiệm vụ:* Kết nối trực tiếp với IP Camera, chạy mô hình học máy cục bộ (YOLOv8 mã hóa tối ưu) để nhận diện biển số xe (ANPR). Xử lý tín hiệu kích hoạt rơ-le mở barrier tự động khi nhận diện đúng hoặc quét mã QR hợp lệ.

### 2.2 Tầng Mạng & Nền tảng (Network & Platform Layer)
* **Giao thức Truyền thông:**
    * **MQTT over TLS (Port 8883):** Sử dụng để truyền gói tin telemetry trạng thái ô đỗ từ cụm ESP32 về hệ thống xử lý trung tâm, đảm bảo độ trễ thấp và tiết kiệm năng lượng cho thiết bị.
    * **HTTPS RESTful API:** Dùng cho toàn bộ luồng nghiệp vụ phía Client (Đăng ký tài khoản, Đặt lịch giữ chỗ, Thanh toán trực tuyến).
* **Backend Server (Java Spring Boot):**
    * *Spring Security & JWT:* Quản lý vòng đời xác thực, cấp phát token và kiểm soát phân quyền nghiêm ngặt.
    * *Spring Data JPA:* Sử dụng để giao tiếp với hệ cơ sở dữ liệu qua tầng Hibernate, tăng tốc độ phát triển.
    * *Eclipse Paho / Spring Integration MQTT:* Đóng vai trò làm MQTT Client nội bộ, tự động subscribe các topic từ Mosquitto Broker và chuyển đổi payload thành các Event trong hệ thống Spring.
* **Database (PostgreSQL):**
    * Hệ quản trị cơ sở dữ liệu quan hệ mạnh mẽ, lưu trữ toàn bộ cấu trúc bãi xe, thông tin người dùng, lịch sử giao dịch.
    * Sử dụng tính năng **Pessimistic Locking (`SELECT ... FOR UPDATE`)** để xử lý bài toán đồng thời (Concurrency), ngăn chặn hoàn toàn hiện tượng Race Condition khi nhiều người dùng cùng nhấn đặt một ô đỗ duy nhất tại một thời điểm.

### 2.3 Tầng Ứng dụng (Application Layer)
* **Mobile App (Dành cho người dùng lái xe):**
    * *Công nghệ:* **Flutter** (Dart) giúp tối ưu hóa hiệu năng trên cả iOS và Android.
    * *Chức năng:* Hiển thị bản đồ bãi xe theo thời gian thực, tìm kiếm ô đỗ trống, đặt chỗ trước (Booking), điều hướng và hiển thị mã QR check-in/check-out tích hợp ví điện tử.
* **Web App (Dành cho Ban quản lý bãi xe):**
    * *Công nghệ:* **ReactJS** + Tailwind CSS.
    * *Chức năng:* Dashboard trực quan hóa trạng thái bãi xe (Xanh: Trống, Đỏ: Có xe, Vàng: Được giữ chỗ trước). Quản lý doanh thu, thống kê lưu lượng xe theo giờ, và hỗ trợ kích hoạt cổng barrier thủ công từ xa qua cơ chế **Spring WebSocket (STOMP)**.

---

## 3. Phương thức Giao tiếp & Luồng Dữ liệu (Data Flow)

### 3.1 Luồng cập nhật trạng thái ô đỗ (Periodic Telemetry)
1. Cảm biến Siêu âm phát hiện xe đổi trạng thái $ightarrow$ ESP32 đóng gói tin JSON: `{"slot_id": "A105", "occupied": true, "timestamp": 1782394800}`.
2. ESP32 đẩy dữ liệu mã hóa lên Mosquitto Broker qua Topic: `parking/slots/A105/status`.
3. Java Backend lắng nghe topic, tiếp nhận gói tin thông qua MQTT Listener, kích hoạt `@Transactional` để cập nhật trạng thái ô đỗ vào cơ sở dữ liệu PostgreSQL.
4. Ngay lập tức, một thông điệp được đẩy thông qua **Spring WebSocket (STOMP)** đến toàn bộ các Web Dashboard và Mobile App đang kết nối để cập nhật giao diện thời gian thực mà không cần reload.

### 3.2 Luồng Đặt chỗ trước & Vào bãi (Booking & Check-in)
1. Người dùng chọn ô trống trên Mobile App, gửi request `POST /api/v1/bookings` kèm JWT Token.
2. Tại tầng Service của Spring Boot, phương thức xử lý cấu hình `@Transactional` sẽ gọi truy vấn PostgreSQL sử dụng **Pessimistic Lock** để khóa dòng (Row-level lock) của ô đỗ được chọn, chuyển trạng thái từ trống sang `RESERVED` trong 20 phút.
3. Hệ thống sinh mã đặt chỗ an toàn, nén thành **QR Code** trả về cho Mobile App.
4. Khi xe đến cổng, camera chụp ảnh biển số (ANPR), đồng thời người dùng quét mã QR tại cổng $ightarrow$ Raspberry Pi gửi dữ liệu xác thực về Endpoint của Spring Boot.
5. Nếu khớp thông tin $ightarrow$ Spring Boot gửi lệnh xuống MQTT Broker qua topic điều khiển cổng `parking/gates/gate1/control` $ightarrow$ Raspberry Pi nhận lệnh và kích hoạt Rơ-le mở Barrier.

---

## 4. Kỹ thuật Bảo mật Hệ thống (Cybersecurity Implementation)

### 4.1 Xác thực & Phân quyền (Authentication & Authorization)
* **Xác thực API bằng JWT:** Ứng dụng mô hình Stateless Authentication với Spring Security. Cấp phát cặp `Access Token` và `Refresh Token` sau khi đăng nhập thành công. Mọi truy cập vào tài nguyên API bắt buộc đi qua bộ lọc `OncePerRequestFilter` để kiểm tra chữ ký token ở HTTP Header.
* **Kiểm soát truy cập dựa trên vai trò (RBAC):** Cấu hình phân quyền chi tiết tại tầng Method bằng Annotation `@PreAuthorize("hasRole('ADMIN')")` hoặc `@PreAuthorize("hasRole('DRIVER')")`.

### 4.2 Bảo mật Tầng Thiết bị IoT & Đường truyền
* **Mã hóa Toàn vẹn (HTTPS & mTLS):**
    * Toàn bộ các API Endpoints công khai bắt buộc chạy trên giao thức **HTTPS (TLS 1.3)**.
    * Giao tiếp giữa mạng lưới thiết bị IoT (ESP32, Raspberry Pi) và Mosquitto Broker triển khai cơ chế **Xác thực hai chiều (Mutual Authentication - mTLS)** bằng chứng chỉ số **X.509**. Thiết bị không mang chứng chỉ được mã hóa và ký bởi CA nội bộ của hệ thống sẽ bị Broker từ chối kết nối ngay từ tầng mạng, ngăn chặn hoàn toàn nguy cơ giả mạo thiết bị (Device Spoofing).

### 4.3 Phòng chống Tấn công mạng & An toàn Dữ liệu
* **Chống tấn công phát lại (Anti-Replay Attack):** Các gói tin ra lệnh điều khiển thiết bị (như lệnh mở cổng barrier) chứa cấu trúc mã hóa bao gồm một chuỗi ngẫu nhiên dùng một lần (**Nonce**) kèm theo nhãn thời gian (**Timestamp**). Java Backend sẽ kiểm tra tính hợp lệ của timestamp (sai lệch không quá 5 giây) và lưu nonce vào Redis/Cache để đối chiếu chống lặp lại lệnh cũ.
* **An toàn dữ liệu nhạy cảm:** Thông tin định danh người dùng và lịch sử di chuyển (biển số xe, hình ảnh check-in) được mã hóa ở mức cơ sở dữ liệu PostgreSQL sử dụng hàm crypto đối xứng **AES-256**. Toàn bộ mật khẩu người dùng được băm mã hóa bằng thuật toán **BCrypt** có Salt độ phức tạp cao trước khi lưu trữ.
* **Kiểm soát tần suất (Rate Limiting):** Sử dụng thư viện **Bucket4j** hoặc cấu hình trên API Gateway để giới hạn số lượng request từ một IP/Tài khoản trong một khoảng thời gian, phòng chống hiệu quả tấn công brute-force và DDoS vào hệ thống Java Backend.
