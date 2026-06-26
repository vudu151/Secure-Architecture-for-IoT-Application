# Báo cáo Kiến trúc & Thiết kế Hệ thống Bãi đỗ xe Thông minh (Smart Parking System) — Version 3

Hệ thống Bãi đỗ xe Thông minh được thiết kế theo mô hình kiến trúc 3 lớp (3-Layer Architecture), tích hợp ứng dụng di động (Mobile App), ứng dụng web quản trị (Web App), phần cứng IoT tự chế tạo và các cơ chế bảo mật nâng cao. Backend xử lý trung tâm được xây dựng trên nền tảng Java Spring Boot kết hợp PostgreSQL.

---

## 1. Giới thiệu Đề tài

### 1.1 Bối cảnh & Động lực

Tình trạng tắc nghẽn giao thông và thiếu hụt chỗ đỗ xe tại các đô thị lớn đang ngày càng trở nên nghiêm trọng. Theo thống kê, trung bình một tài xế mất 15–20 phút mỗi lần tìm kiếm chỗ đỗ, gây lãng phí thời gian, nhiên liệu và gia tăng phát thải khí CO2. Hệ thống Smart Parking giải quyết bài toán này bằng cách cho phép người dùng tra cứu và đặt chỗ đỗ xe trước thông qua ứng dụng di động, kết hợp phần cứng IoT giám sát trạng thái ô đỗ theo thời gian thực.

### 1.2 Phạm vi Hệ thống

- **Quy mô:** 1 bãi đỗ xe mẫu với 20–50 ô đỗ, 1 cổng vào, 1 cổng ra
- **Đối tượng người dùng:** Tài xế (đặt chỗ, check-in/out), Quản trị viên (giám sát, thống kê)
- **Phần mềm:** Backend API, Web Dashboard, Mobile App
- **Phần cứng:** ESP32 + cảm biến tại mỗi ô đỗ, Raspberry Pi + Camera tại cổng

---

## 2. Kiến trúc Tổng quan Hệ thống (YÊU CẦU 1)

### 2.1 Sơ đồ Kiến trúc Hệ thống

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     TẦNG ỨNG DỤNG (APPLICATION LAYER)                      │
│                                                                             │
│   ┌─────────────────────┐              ┌──────────────────────────┐         │
│   │  Flutter Mobile App  │              │    ReactJS Web App       │         │
│   │  ─────────────────── │              │  ──────────────────────  │         │
│   │  • Đặt chỗ & QR Code│              │  • Dashboard giám sát   │         │
│   │  • Bản đồ realtime   │              │  • Quản lý doanh thu    │         │
│   │  • Thanh toán        │              │  • Điều khiển barrier    │         │
│   └────────┬─────────────┘              └─────────────┬────────────┘        │
│            │                                          │                     │
└────────────┼──────────────────────────────────────────┼─────────────────────┘
             │ HTTPS + JWT                              │ HTTPS + JWT
             │ WebSocket (STOMP)                        │ WebSocket (STOMP)
             ▼                                          ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                  TẦNG MẠNG & NỀN TẢNG (NETWORK & PLATFORM LAYER)           │
│                                                                             │
│   ┌─────────────────────────────────────────────────────────────────┐       │
│   │              API Gateway (Nginx Reverse Proxy)                  │       │
│   │              • TLS Termination  • Rate Limiting  • CORS         │       │
│   └──────────────────────────────┬──────────────────────────────────┘       │
│                                  │                                          │
│   ┌──────────────────────────────▼──────────────────────────────────┐       │
│   │           Backend Server: Java Spring Boot                      │       │
│   │           • Spring Security + JWT Filter                        │       │
│   │           • Spring Data JPA (Hibernate)                         │       │
│   │           • Spring WebSocket (STOMP)                            │       │
│   │           • Eclipse Paho MQTT Client                            │       │
│   │           • Spring Validation + AOP Logging                     │       │
│   └───────┬──────────────┬──────────────┬──────────────┬────────────┘       │
│           │              │              │              │                     │
│           ▼              ▼              ▼              ▼                     │
│   ┌────────────┐  ┌────────────┐  ┌──────────┐  ┌───────────┐              │
│   │ PostgreSQL │  │   Redis    │  │Mosquitto │  │  MinIO/   │              │
│   │ ────────── │  │ ────────── │  │ (MQTT    │  │  Local FS │              │
│   │ • User     │  │ • JWT      │  │  Broker) │  │ ────────  │              │
│   │ • Booking  │  │   Blacklist│  │ • TLS    │  │ • Ảnh     │              │
│   │ • Slot     │  │ • Nonce    │  │ • mTLS   │  │   biển số │              │
│   │ • Payment  │  │   Store    │  │ • ACL    │  │ • QR Code │              │
│   │ • AES-256  │  │ • Rate     │  │          │  │           │              │
│   │   encrypt  │  │   Counter  │  │          │  │           │              │
│   └────────────┘  └────────────┘  └────┬─────┘  └───────────┘              │
│                                        │                                    │
└────────────────────────────────────────┼────────────────────────────────────┘
                                         │ MQTT over TLS (Port 8883)
                                         │ mTLS + X.509 Certificate
                                         ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      TẦNG THIẾT BỊ (DEVICE LAYER)                          │
│                                                                             │
│   ┌─────────────────────────────────────────────────────────────────┐       │
│   │              Raspberry Pi 4 (Edge Gateway — Cổng vào/ra)        │       │
│   │              • IP Camera → ANPR (YOLOv8 Nano + ONNX Runtime)    │       │
│   │              • QR Code Scanner                                  │       │
│   │              • Servo/Relay → Điều khiển Barrier                 │       │
│   │              • MQTT Client (mTLS)                               │       │
│   └──────────────────────────────┬──────────────────────────────────┘       │
│                                  │ Wi-Fi / GPIO                             │
│                                  ▼                                          │
│   ┌─────────────────────────────────────────────────────────────────┐       │
│   │              Cụm ESP32 Nodes (Tại mỗi ô đỗ xe)                 │       │
│   │              • Cảm biến siêu âm HC-SR04                        │       │
│   │              • LED trạng thái (Xanh/Đỏ/Vàng)                   │       │
│   │              • MQTT Client (TLS + Certificate Auth)             │       │
│   │              • Deep Sleep Mode tiết kiệm năng lượng             │       │
│   └─────────────────────────────────────────────────────────────────┘       │
│                                                                             │
│   ┌─────────────────────────────────────────────────────────────────┐       │
│   │              IP Camera (Cổng vào / Cổng ra)                     │       │
│   │              • RTSP Stream → Raspberry Pi                       │       │
│   └─────────────────────────────────────────────────────────────────┘       │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 Bảng Tổng hợp Technology Stack

| Thành phần | Công nghệ | Phiên bản | Lý do chọn |
|------------|-----------|-----------|-------------|
| **Backend Server** | Java Spring Boot | 3.x | Hệ sinh thái mạnh, Spring Security tích hợp sẵn, JPA hỗ trợ PostgreSQL tốt |
| **Database** | PostgreSQL | 16.x | RDBMS mạnh, hỗ trợ Row-level locking, pgcrypto cho mã hóa dữ liệu |
| **Cache / Session** | Redis | 7.x | In-memory store tốc độ cao, dùng cho JWT Blacklist, Nonce store, Rate limiting |
| **MQTT Broker** | Eclipse Mosquitto | 2.x | Nhẹ, hỗ trợ TLS/mTLS, ACL, phù hợp cho IoT gateway |
| **API Gateway** | Nginx | 1.25+ | Reverse proxy hiệu năng cao, TLS termination, rate limiting |
| **Mobile App** | Flutter (Dart) | 3.x | Cross-platform (iOS + Android), hiệu năng gần native |
| **Web App** | ReactJS + Ant Design | 18.x | Component-rich, Dashboard-friendly, hệ sinh thái lớn |
| **Edge Gateway** | Raspberry Pi 4 (4GB) | — | Đủ mạnh chạy ML inference, GPIO điều khiển barrier |
| **Sensor Node** | ESP32-WROOM-32 | — | Wi-Fi tích hợp, hardware crypto, Deep Sleep, giá rẻ |
| **Cảm biến** | HC-SR04 (Siêu âm) | — | Phát hiện vật thể đơn giản, đáng tin cậy, giá thành thấp |
| **Camera** | IP Camera (RTSP) | — | Cung cấp video stream cho ANPR |
| **ANPR Model** | YOLOv8 Nano + ONNX | — | Nhẹ, tối ưu cho edge device, độ chính xác cao |
| **File Storage** | MinIO hoặc Local FS | — | Lưu ảnh biển số, QR Code |
| **Containerization** | Docker + Docker Compose | — | Đóng gói và triển khai đồng nhất tất cả services |

### 2.3 Mô tả Chi tiết Các Thành phần

#### 2.3.1 Tầng Thiết bị (Device Layer)

**ESP32 Nodes (Tại mỗi vị trí đỗ):**
- *Nhiệm vụ:* Đọc dữ liệu từ cảm biến siêu âm HC-SR04 theo chu kỳ (mỗi 2–5 giây) để xác định sự hiện diện của phương tiện tại ô đỗ. Điều khiển LED trạng thái.
- *Lý do chọn:* Chip xử lý lõi kép 32-bit, tích hợp Wi-Fi/BLE, hỗ trợ phần cứng tăng tốc mã hóa (AES, SHA-2, RSA), chế độ Deep Sleep tiết kiệm năng lượng.
- *Giao tiếp:* Gửi telemetry qua MQTT over TLS, xác thực bằng chứng chỉ X.509.

**Raspberry Pi 4 (Edge Gateway tại Cổng ra/vào):**
- *Nhiệm vụ:* Kết nối IP Camera qua RTSP, chạy mô hình YOLOv8 Nano (tối ưu ONNX Runtime) để nhận diện biển số xe (ANPR). Đọc QR Code từ scanner. Điều khiển Servo/Relay mở barrier.
- *Lý do chọn:* CPU ARM Cortex-A72 quad-core, 4GB RAM, đủ mạnh cho ML inference cục bộ.

**IP Camera:**
- *Nhiệm vụ:* Cung cấp video stream RTSP cho Raspberry Pi xử lý ANPR.

#### 2.3.2 Tầng Mạng & Nền tảng (Network & Platform Layer)

**Mosquitto MQTT Broker:**
- *Nhiệm vụ:* Trung tâm truyền nhận tin nhắn giữa thiết bị IoT và Backend.
- *Cấu hình bảo mật:* TLS 1.3, mTLS (Mutual Authentication) với chứng chỉ X.509, ACL phân quyền topic cho từng thiết bị.

**Java Spring Boot Backend:**
- *Spring Security + JWT:* Quản lý xác thực, cấp phát Access Token (15 phút) + Refresh Token (7 ngày). Filter `OncePerRequestFilter` kiểm tra JWT trên mọi request.
- *Spring Data JPA:* Tương tác PostgreSQL qua Hibernate ORM, sử dụng Pessimistic Locking cho booking đồng thời.
- *Spring WebSocket (STOMP):* Đẩy cập nhật real-time đến Web Dashboard và Mobile App.
- *Eclipse Paho MQTT Client:* Subscribe các topic từ Mosquitto, chuyển payload thành Spring Event.
- *Spring Validation:* Validate input `@Valid`, chống SQL Injection, XSS.
- *Spring AOP:* Audit logging cho các hành động quan trọng.

**PostgreSQL:**
- Lưu trữ toàn bộ dữ liệu nghiệp vụ: Users, Vehicles, ParkingSlots, Bookings, Transactions, DeviceRegistry.
- **Pessimistic Locking (`SELECT ... FOR UPDATE`)** cho xử lý đồng thời đặt chỗ.
- **pgcrypto (AES-256)** mã hóa dữ liệu nhạy cảm (biển số xe, hình ảnh).

**Redis:**
- JWT Blacklist (lưu token đã revoke cho đến khi hết hạn).
- Nonce Store (chống Anti-Replay Attack, TTL 10 giây).
- Rate Limiting Counter (Bucket4j backend).

**Nginx API Gateway:**
- TLS Termination (chứng chỉ Let's Encrypt).
- Rate Limiting (limit_req_zone).
- CORS headers.
- Reverse proxy đến Spring Boot.

#### 2.3.3 Tầng Ứng dụng (Application Layer)

**Mobile App — Flutter (Dành cho Tài xế):**
- Đăng ký / Đăng nhập (JWT Auth)
- Xem bản đồ bãi xe real-time (WebSocket)
- Tìm kiếm ô đỗ trống, đặt chỗ trước (Booking)
- Hiển thị QR Code check-in / check-out
- Thanh toán (ví nội bộ / mô phỏng)
- Lịch sử đặt chỗ & giao dịch
- Điều hướng đến bãi xe

**Web App — ReactJS + Ant Design (Dành cho Quản trị viên):**
- Dashboard trực quan: trạng thái ô đỗ (Xanh: Trống, Đỏ: Có xe, Vàng: Đã đặt)
- Thống kê doanh thu, lưu lượng xe theo giờ/ngày/tháng (biểu đồ)
- Quản lý người dùng, phương tiện
- Điều khiển barrier thủ công từ xa (WebSocket)
- Xem log sự kiện, cảnh báo bảo mật
- Quản lý thiết bị IoT (trạng thái online/offline)

### 2.4 Phương thức Giao tiếp & Protocol Matrix

| Kết nối | Giao thức | Port | Bảo mật | Mục đích |
|---------|-----------|------|---------|----------|
| Mobile/Web → API Gateway | HTTPS | 443 | TLS 1.3 + JWT | REST API calls |
| Mobile/Web → Backend | WebSocket (WSS) | 443 | TLS 1.3 + JWT | Real-time updates (STOMP) |
| API Gateway → Spring Boot | HTTP | 8080 | Internal network | Reverse proxy |
| Spring Boot → PostgreSQL | TCP | 5432 | SSL + Password | Database queries |
| Spring Boot → Redis | TCP | 6379 | Password + TLS | Cache operations |
| Spring Boot ↔ Mosquitto | MQTT | 1883 | Internal network | Pub/Sub internal |
| ESP32 → Mosquitto | MQTT over TLS | 8883 | mTLS + X.509 | Sensor telemetry |
| Raspberry Pi → Mosquitto | MQTT over TLS | 8883 | mTLS + X.509 | Gate control commands |
| Raspberry Pi → IP Camera | RTSP | 554 | Local network | Video stream |

---

## 3. Thiết kế Cơ sở Dữ liệu (ERD)

### 3.1 Sơ đồ ERD

```
┌──────────────┐       ┌──────────────────┐       ┌───────────────┐
│    users     │       │    vehicles      │       │ parking_slots │
│──────────────│       │──────────────────│       │───────────────│
│ id (PK)      │◄──┐   │ id (PK)          │       │ id (PK)       │
│ email        │   │   │ user_id (FK)     │───┐   │ slot_code     │
│ password_hash│   │   │ license_plate    │   │   │ zone          │
│ full_name    │   │   │ plate_encrypted  │   │   │ status        │
│ phone        │   └───│ vehicle_type     │   │   │ sensor_id     │
│ role         │       │ is_default       │   │   │ created_at    │
│ is_active    │       │ created_at       │   │   └───────┬───────┘
│ created_at   │       └──────────────────┘   │           │
│ updated_at   │                               │           │
└──────┬───────┘                               │           │
       │                                       │           │
       │  ┌────────────────────────┐           │           │
       │  │      bookings         │           │           │
       │  │────────────────────────│           │           │
       └──│ id (PK)               │           │           │
          │ user_id (FK)          │           │           │
          │ vehicle_id (FK)       │◄──────────┘           │
          │ slot_id (FK)          │◄───────────────────────┘
          │ booking_code          │
          │ qr_code_data          │
          │ status                │  (PENDING → CONFIRMED → CHECKED_IN
          │ booked_from           │   → CHECKED_OUT → COMPLETED / CANCELLED / EXPIRED)
          │ booked_until          │
          │ checked_in_at         │
          │ checked_out_at        │
          │ total_amount          │
          │ created_at            │
          └───────────┬───────────┘
                      │
          ┌───────────▼───────────┐       ┌──────────────────────┐
          │    transactions       │       │   device_registry    │
          │───────────────────────│       │──────────────────────│
          │ id (PK)               │       │ id (PK)              │
          │ booking_id (FK)       │       │ device_uid           │
          │ user_id (FK)          │       │ device_type          │
          │ amount                │       │ location             │
          │ payment_method        │       │ certificate_cn       │
          │ payment_status        │       │ is_online            │
          │ transaction_ref       │       │ last_heartbeat       │
          │ created_at            │       │ firmware_version     │
          └───────────────────────┘       │ created_at           │
                                          └──────────────────────┘
          ┌───────────────────────┐
          │   security_audit_log  │
          │───────────────────────│
          │ id (PK)               │
          │ user_id (FK, nullable)│
          │ action                │
          │ resource              │
          │ ip_address            │
          │ details (JSONB)       │
          │ created_at            │
          └───────────────────────┘
```

### 3.2 Bảng Trạng thái

**Parking Slot Status:**
| Trạng thái | Ý nghĩa | Màu hiển thị |
|------------|----------|-------------|
| `AVAILABLE` | Ô trống, có thể đặt | 🟢 Xanh |
| `OCCUPIED` | Có xe đang đỗ | 🔴 Đỏ |
| `RESERVED` | Đã được đặt trước (chờ check-in) | 🟡 Vàng |
| `MAINTENANCE` | Đang bảo trì | ⚪ Xám |

**Booking Status Flow:**
```
PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT → COMPLETED
    │          │
    ▼          ▼
 EXPIRED   CANCELLED
```

---

## 4. Thiết kế API Endpoints (YÊU CẦU 2)

### 4.1 Authentication APIs

| Method | Endpoint | Mô tả | Auth |
|--------|----------|--------|------|
| POST | `/api/v1/auth/register` | Đăng ký tài khoản | Public |
| POST | `/api/v1/auth/login` | Đăng nhập, nhận JWT | Public |
| POST | `/api/v1/auth/refresh` | Làm mới Access Token | Refresh Token |
| POST | `/api/v1/auth/logout` | Đăng xuất, blacklist token | JWT |

### 4.2 Parking Slot APIs

| Method | Endpoint | Mô tả | Auth |
|--------|----------|--------|------|
| GET | `/api/v1/slots` | Danh sách ô đỗ + trạng thái | JWT (DRIVER/ADMIN) |
| GET | `/api/v1/slots/{id}` | Chi tiết 1 ô đỗ | JWT |
| PUT | `/api/v1/slots/{id}/status` | Cập nhật trạng thái (từ IoT) | Internal / mTLS |
| GET | `/api/v1/slots/available` | Lọc ô đỗ trống theo thời gian | JWT (DRIVER) |

### 4.3 Booking APIs

| Method | Endpoint | Mô tả | Auth |
|--------|----------|--------|------|
| POST | `/api/v1/bookings` | Tạo đặt chỗ mới | JWT (DRIVER) |
| GET | `/api/v1/bookings/my` | Lịch sử đặt chỗ của tôi | JWT (DRIVER) |
| GET | `/api/v1/bookings/{id}` | Chi tiết 1 booking | JWT |
| POST | `/api/v1/bookings/{id}/cancel` | Hủy đặt chỗ | JWT (DRIVER) |
| POST | `/api/v1/bookings/{id}/check-in` | Check-in (quét QR) | JWT / Internal |
| POST | `/api/v1/bookings/{id}/check-out` | Check-out | JWT / Internal |

### 4.4 Payment APIs

| Method | Endpoint | Mô tả | Auth |
|--------|----------|--------|------|
| GET | `/api/v1/wallet/balance` | Xem số dư ví | JWT (DRIVER) |
| POST | `/api/v1/wallet/topup` | Nạp tiền ví (mô phỏng) | JWT (DRIVER) |
| POST | `/api/v1/payments/process` | Xử lý thanh toán booking | JWT (DRIVER) |
| GET | `/api/v1/transactions/my` | Lịch sử giao dịch | JWT (DRIVER) |

### 4.5 Admin APIs

| Method | Endpoint | Mô tả | Auth |
|--------|----------|--------|------|
| GET | `/api/v1/admin/dashboard` | Thống kê tổng quan | JWT (ADMIN) |
| GET | `/api/v1/admin/revenue` | Báo cáo doanh thu | JWT (ADMIN) |
| GET | `/api/v1/admin/users` | Quản lý người dùng | JWT (ADMIN) |
| GET | `/api/v1/admin/devices` | Quản lý thiết bị IoT | JWT (ADMIN) |
| POST | `/api/v1/admin/gate/{id}/control` | Điều khiển barrier thủ công | JWT (ADMIN) |
| GET | `/api/v1/admin/audit-logs` | Xem Security audit logs | JWT (ADMIN) |

### 4.6 Device / IoT Internal APIs

| Method | Endpoint | Mô tả | Auth |
|--------|----------|--------|------|
| POST | `/api/v1/devices/verify-plate` | Xác thực biển số (từ RPi) | mTLS Certificate |
| POST | `/api/v1/devices/verify-qr` | Xác thực QR Code (từ RPi) | mTLS Certificate |
| POST | `/api/v1/devices/heartbeat` | Thiết bị báo cáo online | mTLS Certificate |

### 4.7 MQTT Topics

| Topic | Publisher | Subscriber | Payload | QoS |
|-------|-----------|------------|---------|-----|
| `parking/slots/{slot_id}/status` | ESP32 | Spring Boot | `{"slot_id","occupied","timestamp"}` | 1 |
| `parking/gates/{gate_id}/control` | Spring Boot | Raspberry Pi | `{"action":"OPEN/CLOSE","nonce","timestamp"}` | 2 |
| `parking/gates/{gate_id}/event` | Raspberry Pi | Spring Boot | `{"event":"PLATE_DETECTED","plate","image_ref"}` | 1 |
| `parking/devices/{device_id}/heartbeat` | ESP32/RPi | Spring Boot | `{"device_id","uptime","rssi"}` | 0 |

---

## 5. Luồng Dữ liệu Chi tiết (YÊU CẦU 2)

### 5.1 Luồng cập nhật trạng thái ô đỗ (Periodic Telemetry)

```
ESP32 (cảm biến)                Mosquitto          Spring Boot            PostgreSQL       WebSocket
     │                            │                     │                     │               │
     │ ──(1) Phát hiện xe──→      │                     │                     │               │
     │ ──(2) MQTT Publish─────→   │                     │                     │               │
     │   Topic: parking/slots/    │                     │                     │               │
     │   A105/status              │                     │                     │               │
     │   {occupied:true}          │                     │                     │               │
     │                            │ ──(3) Forward────→  │                     │               │
     │                            │                     │ ──(4) UPDATE────→   │               │
     │                            │                     │   slot SET status   │               │
     │                            │                     │   = 'OCCUPIED'      │               │
     │                            │                     │                     │               │
     │                            │                     │ ──(5) STOMP Push──────────────────→ │
     │                            │                     │   /topic/slots      │          Mobile/Web
     │                            │                     │                     │          cập nhật UI
```

### 5.2 Luồng Đặt chỗ & Vào bãi (Booking & Check-in)

```
Mobile App        Spring Boot          PostgreSQL          Mosquitto        Raspberry Pi
    │                  │                    │                   │                 │
    │ ──(1) POST ──→   │                    │                   │                 │
    │  /bookings       │                    │                   │                 │
    │  + JWT Token     │                    │                   │                 │
    │                  │ ──(2) BEGIN TX──→   │                   │                 │
    │                  │  SELECT ... FOR     │                   │                 │
    │                  │  UPDATE (Lock ô)   │                   │                 │
    │                  │                    │                   │                 │
    │                  │ ──(3) UPDATE slot   │                   │                 │
    │                  │  → RESERVED        │                   │                 │
    │                  │  INSERT booking    │                   │                 │
    │                  │  COMMIT TX ────→   │                   │                 │
    │                  │                    │                   │                 │
    │ ◀──(4) QR Code── │                    │                   │                 │
    │   + booking_code │                    │                   │                 │
    │                  │                    │                   │                 │
    │ ═══════ Xe đến cổng bãi ═══════════════════════════════════                │
    │                  │                    │                   │                 │
    │                  │                    │                   │  ◀──(5) Camera  │
    │                  │                    │                   │    chụp biển số │
    │                  │                    │                   │                 │
    │                  │                    │                   │ ◀──(6) MQTT──── │
    │                  │                    │                   │  plate_detected │
    │                  │ ◀──(7) Forward──── │                   │                 │
    │                  │                    │                   │                 │
    │                  │ ──(8) Verify plate  │                   │                 │
    │                  │  + booking match──→│                   │                 │
    │                  │                    │                   │                 │
    │                  │ ──(9) UPDATE──────→│                   │                 │
    │                  │  booking → CHECKED_IN                  │                 │
    │                  │                    │                   │                 │
    │                  │ ──(10) MQTT Publish─────────────────→  │                 │
    │                  │  gate/control:OPEN │                   │ ──(11) OPEN──→  │
    │                  │  {nonce,timestamp} │                   │   Barrier       │
```

### 5.3 Luồng Check-out & Thanh toán

```
Raspberry Pi       Mosquitto         Spring Boot         PostgreSQL        Mobile App
    │                  │                  │                   │                │
    │ ──(1) Camera ──→ │                  │                   │                │
    │  detect plate    │                  │                   │                │
    │  leaving         │ ──(2) Forward──→ │                   │                │
    │                  │                  │                   │                │
    │                  │                  │ ──(3) Tìm booking │                │
    │                  │                  │  CHECKED_IN ──→   │                │
    │                  │                  │                   │                │
    │                  │                  │ ──(4) Tính phí ─→ │                │
    │                  │                  │  (thời gian đỗ    │                │
    │                  │                  │   × đơn giá)      │                │
    │                  │                  │                   │                │
    │                  │                  │ ──(5) Trừ ví ──→  │                │
    │                  │                  │  UPDATE wallet    │                │
    │                  │                  │  INSERT transaction│               │
    │                  │                  │  booking→COMPLETED│                │
    │                  │                  │  slot→AVAILABLE   │                │
    │                  │                  │                   │                │
    │                  │ ◀──(6) MQTT ──── │                   │                │
    │ ◀──(7) Barrier── │  gate:OPEN       │                   │                │
    │  OPEN            │                  │                   │                │
    │                  │                  │ ──(8) Push ──────────────────────→ │
    │                  │                  │  Thông báo hoàn tất│          Notification
    │                  │                  │  + hóa đơn        │                │
```

---

## 6. Kỹ thuật Bảo mật Hệ thống (YÊU CẦU 3)

### 6.1 Threat Model — Các Mối Đe dọa & Biện pháp

| # | Mối đe dọa | Tầng | Kỹ thuật tấn công | Biện pháp đối phó |
|---|-----------|------|-------------------|-------------------|
| T1 | Đánh cắp tài khoản | App | Brute-force login, Credential stuffing | BCrypt hash, Rate Limiting, Account Lockout |
| T2 | Chiếm đoạt phiên | Network | Token theft, Session hijacking | JWT short-lived (15min), Refresh Token rotation, JWT Blacklist |
| T3 | Giả mạo thiết bị IoT | Device | Device spoofing | mTLS + X.509 Certificate, Device Registry |
| T4 | Tấn công phát lại | Network | Replay attack (lệnh mở cổng) | Nonce + Timestamp (±5s) + Redis store |
| T5 | Nghe lén dữ liệu | Network | Man-in-the-Middle (MITM) | TLS 1.3 (HTTPS), MQTT over TLS |
| T6 | SQL Injection | App/DB | Malicious SQL input | Spring Data JPA (Parameterized queries), @Valid input |
| T7 | Cross-Site Scripting | Web | XSS payload injection | Content Security Policy, Input sanitization, React auto-escape |
| T8 | Lộ dữ liệu nhạy cảm | DB | Database breach | AES-256 encryption (pgcrypto), BCrypt password |
| T9 | DDoS / Abuse | Network | Flood requests | Rate Limiting (Nginx + Bucket4j), API Gateway |
| T10 | Firmware giả mạo | Device | Malicious OTA update | Signed firmware (ESP32 Secure Boot) |
| T11 | Truy cập trái phép API | App | Unauthorized access | RBAC (@PreAuthorize), JWT scope validation |
| T12 | Race Condition đặt chỗ | DB | Concurrent booking | Pessimistic Lock (SELECT ... FOR UPDATE) |

### 6.2 Xác thực & Phân quyền (Authentication & Authorization)

**JWT Authentication Flow:**
```
Client                    Spring Boot                      Redis
  │                           │                              │
  │ ──(1) POST /auth/login──→ │                              │
  │   {email, password}       │                              │
  │                           │ ──(2) Verify BCrypt ──→      │
  │                           │                              │
  │ ◀──(3) 200 OK ─────────  │                              │
  │   {access_token (15min),  │                              │
  │    refresh_token (7d)}    │                              │
  │                           │                              │
  │ ──(4) GET /api/v1/slots── │                              │
  │   Authorization: Bearer   │                              │
  │   <access_token>          │                              │
  │                           │ ──(5) Check Blacklist ──→    │
  │                           │ ◀── Not blacklisted ────     │
  │                           │                              │
  │                           │ ──(6) Validate JWT ──→       │
  │                           │   (Signature + Expiry        │
  │                           │    + Role extraction)        │
  │                           │                              │
  │ ◀──(7) 200 OK ─────────  │                              │
  │   {slots data}            │                              │
  │                           │                              │
  │ ──(8) POST /auth/logout─→ │                              │
  │                           │ ──(9) Add to Blacklist ──→   │
  │                           │   (TTL = remaining expiry)   │
  │ ◀──(10) 200 OK ────────  │                              │
```

**RBAC Matrix:**
| Resource | DRIVER | ADMIN |
|----------|--------|-------|
| View available slots | ✅ | ✅ |
| Create booking | ✅ | ❌ |
| View own bookings | ✅ | ❌ |
| View all bookings | ❌ | ✅ |
| Dashboard statistics | ❌ | ✅ |
| Manual gate control | ❌ | ✅ |
| User management | ❌ | ✅ |
| Device management | ❌ | ✅ |
| Audit logs | ❌ | ✅ |

### 6.3 Bảo mật Tầng Thiết bị IoT & Đường truyền

**mTLS (Mutual TLS) với X.509:**
- Hệ thống triển khai CA nội bộ (Internal Certificate Authority) sử dụng OpenSSL.
- Mỗi thiết bị (ESP32, Raspberry Pi) được cấp chứng chỉ số X.509 riêng, ký bởi CA nội bộ.
- Mosquitto Broker cấu hình `require_certificate true`, chỉ chấp nhận kết nối từ thiết bị có chứng chỉ hợp lệ.
- Thiết bị không mang chứng chỉ hoặc mang chứng chỉ giả mạo bị từ chối ngay tại tầng TLS handshake → ngăn chặn Device Spoofing.

**MQTT ACL (Access Control List):**
- Mỗi thiết bị chỉ được publish/subscribe trên topic được phân quyền.
- Ví dụ: ESP32 node "slot_A105" chỉ được publish lên `parking/slots/A105/status`, không thể truy cập topic của slot khác.

**Secure Boot & Signed Firmware (ESP32):**
- Kích hoạt ESP32 Secure Boot v2: chỉ cho phép chạy firmware được ký bằng private key.
- Firmware OTA update phải được ký trước khi flash → chống firmware giả mạo.

### 6.4 Phòng chống Tấn công & An toàn Dữ liệu

**Anti-Replay Attack:**
- Các lệnh điều khiển thiết bị (mở barrier) chứa cấu trúc: `{command, nonce (UUID), timestamp}`.
- Backend kiểm tra: timestamp sai lệch ≤ 5 giây so với server time.
- Nonce được lưu vào Redis với TTL = 10 giây. Nonce đã tồn tại → reject (chống lặp lại lệnh cũ).

**Mã hóa Dữ liệu Nhạy cảm:**
- Biển số xe, hình ảnh check-in: mã hóa AES-256 (pgcrypto) trước khi lưu PostgreSQL.
- Mật khẩu người dùng: hash BCrypt (cost factor = 12) với salt ngẫu nhiên.
- JWT secret key: lưu trong biến môi trường, không hard-code.

**Rate Limiting:**
- Tầng Nginx: `limit_req_zone` giới hạn 100 req/min per IP.
- Tầng Spring Boot: Bucket4j giới hạn 10 login attempts / 5 phút / tài khoản → chống brute-force.
- Vượt ngưỡng → HTTP 429 Too Many Requests.

**Input Validation & Injection Prevention:**
- Spring Validation `@Valid` trên mọi DTO request.
- Spring Data JPA sử dụng Parameterized Queries (chống SQL Injection).
- Web App: React tự động escape HTML output (chống XSS). Cấu hình Content-Security-Policy header.

**CORS Policy:**
- Chỉ cho phép origin từ domain Web App và Mobile App.
- Cấu hình trong Spring Boot `WebMvcConfigurer.addCorsMappings()`.

**Security Audit Logging:**
- Ghi log tất cả: đăng nhập (thành công/thất bại), thay đổi quyền, điều khiển barrier, thao tác CRUD quan trọng.
- Sử dụng Spring AOP `@Around` annotation trên các method cần audit.
- Lưu vào bảng `security_audit_log` (PostgreSQL) với JSONB details.

---

## 7. Deployment Architecture

### 7.1 Docker Compose Stack

```
┌─────────────────────────────────────────────────┐
│                 Docker Host (Server)             │
│                                                  │
│  ┌──────────┐ ┌──────────┐ ┌──────────────────┐ │
│  │  Nginx   │ │ Spring   │ │   PostgreSQL     │ │
│  │ (Gateway)│→│  Boot    │→│   (Port 5432)    │ │
│  │ Port 443 │ │ Port 8080│ │                  │ │
│  └──────────┘ └────┬─────┘ └──────────────────┘ │
│                    │                             │
│               ┌────▼─────┐ ┌──────────────────┐ │
│               │  Redis   │ │   Mosquitto      │ │
│               │Port 6379 │ │ Port 8883 (TLS)  │ │
│               └──────────┘ └──────────────────┘ │
└─────────────────────────────────────────────────┘
```

### 7.2 Cấu hình môi trường

| Service | Image | Tài nguyên tối thiểu |
|---------|-------|---------------------|
| Nginx | nginx:1.25-alpine | 128MB RAM |
| Spring Boot | openjdk:17-slim | 512MB RAM |
| PostgreSQL | postgres:16-alpine | 256MB RAM |
| Redis | redis:7-alpine | 128MB RAM |
| Mosquitto | eclipse-mosquitto:2 | 64MB RAM |

---

## 8. Kết luận & Hướng phát triển

### 8.1 Tổng kết
Hệ thống Smart Parking đáp ứng đầy đủ 3 yêu cầu đề tài:
1. **Kiến trúc:** 3 lớp rõ ràng với sơ đồ, bảng công nghệ, protocol matrix.
2. **Triển khai:** Backend (Spring Boot), Web App (ReactJS), Mobile App (Flutter), Phần cứng IoT (ESP32 + Raspberry Pi).
3. **Bảo mật:** JWT Auth, RBAC, mTLS/X.509, Anti-Replay, AES-256, BCrypt, Rate Limiting, Audit Logging.

### 8.2 Hướng phát triển tương lai
- Tích hợp cổng thanh toán thực (VNPay, MoMo).
- Mở rộng sang hệ thống nhiều bãi xe (Multi-parking).
- Áp dụng Machine Learning dự đoán lưu lượng xe.
- Tích hợp IoT Platform (ThingsBoard / AWS IoT Core) cho quản lý thiết bị quy mô lớn.
- Triển khai Kubernetes cho auto-scaling.
