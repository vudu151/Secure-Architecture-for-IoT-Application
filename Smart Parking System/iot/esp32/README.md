# ESP32 Parking Slot Node — Hướng dẫn Khởi chạy

Thư mục này chứa mã nguồn C++ (PlatformIO) và sơ đồ mạch Wokwi của một **nút cảm biến ô đỗ xe** chạy trên ESP32.

---

## 1. Yêu cầu Công cụ

Cài đặt trong **VS Code** hai extension sau:

| Extension | Chức năng |
|---|---|
| **PlatformIO IDE** | Biên dịch & nạp code C++ cho ESP32 |
| **Wokwi Simulator** | Chạy giả lập mạch điện trực quan |

---

## 2. Hai chế độ Build

Dự án sử dụng hệ thống **PlatformIO Multi-Environment** để tự động chuyển đổi giữa môi trường giả lập và phần cứng thật mà **không cần sửa code**:

| Chế độ | Environment | Broker | WiFi | Bảo mật |
|---|---|---|---|---|
| Wokwi Simulation | `wokwi` | HiveMQ (public) | `Wokwi-GUEST` / *(none)* | Không TLS |
| Real Hardware | `production` | Mosquitto Docker | WiFi thật | mTLS (X.509) |

---

## 3. Chạy giả lập Wokwi (Chế độ phát triển)

### Bước 1: Biên dịch

Mở Terminal tại thư mục `esp32` và chạy:

```powershell
# Windows — dùng đường dẫn đầy đủ nếu 'pio' chưa có trong PATH
& "$env:USERPROFILE\.platformio\penv\Scripts\pio.exe" run -e wokwi
```

Sau khi thấy `[SUCCESS]`, file firmware được tạo tại: `.pio/build/wokwi/firmware.elf`

### Bước 2: Khởi động Wokwi

`Ctrl + Shift + P` → `Wokwi: Start Simulator`

### Bước 3: Kiểm tra MQTT

1. Mở **[HiveMQ Web Client](http://www.hivemq.com/demos/websocket-client/)** → nhấn **Connect**.
2. Subscribe topic: `smartpkg/#`
3. Click chuột vào cảm biến **HC-SR04** trên Wokwi và điều chỉnh khoảng cách:
   - **< 30 cm** → Đèn ĐỎ, message `occupied: true`
   - **>= 30 cm** → Đèn XANH, message `occupied: false`

---

## 4. Chạy trên phần cứng thật (Chế độ Production)

### Bước 1: Tạo chứng chỉ mTLS

```bash
# Chạy từ thư mục gốc của dự án
bash docker/certs/generate_certs.sh
```

### Bước 2: Điền chứng chỉ vào header files

Copy nội dung từ thư mục `docker/certs/` vào các file sau trong `src/certs/`:

| File cần điền | Nguồn từ |
|---|---|
| `src/certs/ca_cert.h` | `docker/certs/ca/ca.crt` |
| `src/certs/client_cert.h` | `docker/certs/clients/A01/client.crt` |
| `src/certs/client_key.h` | `docker/certs/clients/A01/client.key` |

> **Lưu ý bảo mật:** Các file này đã được thêm vào `.gitignore` và sẽ KHÔNG được push lên Git.

### Bước 3: Điền thông tin WiFi và IP Broker

Mở **`platformio.ini`**, tìm phần `[env:production]` và cập nhật:

```ini
-D REAL_WIFI_SSID=\"TEN_WIFI_CUA_BAN\"
-D REAL_WIFI_PASSWORD=\"MAT_KHAU_WIFI\"
-D REAL_MQTT_HOST=\"192.168.X.X\"   ; IP máy chạy Docker Mosquitto
```

### Bước 4: Biên dịch và nạp code

```powershell
# Kết nối ESP32 qua USB, sau đó chạy:
& "$env:USERPROFILE\.platformio\penv\Scripts\pio.exe" run -e production --target upload
```

---

## 5. Cấu trúc file quan trọng

```
esp32/
├── src/
│   ├── main.cpp          # Logic chính (dùng #ifdef PRODUCTION_MODE)
│   ├── config.h          # Cấu hình WiFi/MQTT (tự động theo môi trường)
│   └── certs/            # Chứng chỉ mTLS (chỉ dùng cho production)
│       ├── ca_cert.h     # CA certificate
│       ├── client_cert.h # Client certificate
│       └── client_key.h  # Private key (gitignored!)
├── diagram.json          # Sơ đồ mạch Wokwi
├── wokwi.toml            # Cấu hình Wokwi Simulator
└── platformio.ini        # Cấu hình PlatformIO (2 environments)
```

---

## 6. Lưu ý

- **MAX_PATH trên Windows**: File `platformio.ini` đã cấu hình `libdeps_dir = ${sysenv.USERPROFILE}/.pio_libdeps` để tránh lỗi đường dẫn quá dài — hoạt động tự động trên mọi máy tính Windows.
- **MQTT Topic**: Wokwi dùng prefix `smartpkg/` để tránh xung đột trên HiveMQ public. Production dùng prefix `parking/` trên Mosquitto riêng.
