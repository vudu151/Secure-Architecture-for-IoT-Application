# BÁO CÁO BÀI 5: LẬP TRÌNH ESP32 GỬI DỮ LIỆU HTTP

**Môn học:** Kiến trúc và Bảo mật cho ứng dụng IoT  
**Bài:** 5 – ESP32 gửi dữ liệu qua giao thức HTTP  
**Công cụ mô phỏng:** [Wokwi Online Simulator](https://wokwi.com)  
**Link bài mô phỏng:** https://wokwi.com/projects/466454070666256385

---

## 1. Mô tả bài toán

Bài thực hành yêu cầu lập trình ESP32 để gửi dữ liệu cảm biến (nhiệt độ, độ ẩm từ DHT22) lên server thông qua giao thức HTTP theo 3 phương thức:

| # | Phương thức | Kiểu đóng gói dữ liệu | Server |
|---|-------------|----------------------|--------|
| a | HTTP GET    | URL-encoded (query string) | https://postman-echo.com/get |
| b | HTTP POST   | URL-encoded (request body) | https://postman-echo.com/post |
| c | HTTP POST   | JSON (request body)        | https://postman-echo.com/post |

---

## 2. Sơ đồ mạch phần cứng

### 2.1 Danh sách linh kiện

| Linh kiện | Số lượng | Mô tả |
|-----------|----------|-------|
| ESP32 DevKit C v4 | 1 | Vi điều khiển chính, tích hợp WiFi |
| DHT22 | 1 | Cảm biến nhiệt độ và độ ẩm |

### 2.2 Bảng kết nối

| Chân DHT22 | Chân ESP32 | Màu dây |
|-----------|-----------|---------|
| VCC (+) | 3V3 | Đỏ |
| DATA | GPIO 15 | Xanh lá |
| GND (-) | GND | Đen |

### 2.3 Sơ đồ mạch trên Wokwi

![Sơ đồ mạch ESP32 + DHT22 trên Wokwi](/home/duvx/.gemini/antigravity/brain/500e09a2-0777-499f-9acd-8694253e8ae4/wokwi_circuit_esp32_1781105325600.png)

*Hình 1: Sơ đồ mạch ESP32 kết nối DHT22 trên Wokwi Simulator*

---

## 3. Code chương trình

### 3.1 File `sketch.ino`

```cpp
/**
 * =====================================================================
 * Bài 5: Lập trình ESP32 gửi dữ liệu qua HTTP
 * Môn học: Kiến trúc và Bảo mật cho ứng dụng IoT
 * =====================================================================
 * Mô tả chương trình:
 *   Chương trình thực hiện 3 loại HTTP request đến server postman-echo.com:
 *
 *   a) HTTP GET request – dữ liệu đóng gói URL-encoded trong query string
 *   b) HTTP POST request – dữ liệu đóng gói URL-encoded trong request body
 *   c) HTTP POST request – dữ liệu đóng gói JSON trong request body
 *
 *   Dữ liệu gửi đi: nhiệt độ (°C) và độ ẩm (%) lấy từ cảm biến DHT22
 *   Chu kỳ gửi: 10 giây / lần
 * =====================================================================
 */

#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include "DHTesp.h"

const char* WIFI_SSID     = "Wokwi-GUEST";
const char* WIFI_PASSWORD = "";
const char* URL_GET  = "https://postman-echo.com/get";
const char* URL_POST = "https://postman-echo.com/post";

#define DHT_PIN          15
#define SEND_INTERVAL_MS 10000

DHTesp dhtSensor;
unsigned long lastSendTime = 0;
float temperature = 0.0;
float humidity    = 0.0;

void connectToWiFi() {
  Serial.println("[WiFi] Dang ket noi WiFi: " + String(WIFI_SSID));
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  int attempt = 0;
  while (WiFi.status() != WL_CONNECTED && attempt < 20) {
    delay(500);
    Serial.print(".");
    attempt++;
  }
  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("\n[WiFi] Ket noi thanh cong!");
    Serial.println("[WiFi] Dia chi IP: " + WiFi.localIP().toString());
  } else {
    Serial.println("\n[WiFi] Ket noi that bai.");
  }
}

bool readDHT22() {
  TempAndHumidity data = dhtSensor.getTempAndHumidity();
  if (!isnan(data.temperature) && !isnan(data.humidity)) {
    temperature = data.temperature;
    humidity    = data.humidity;
    Serial.println("[DHT22] Nhiet do: " + String(temperature, 1) + " C");
    Serial.println("[DHT22] Do am:    " + String(humidity, 1) + " %");
    return true;
  }
  Serial.println("[DHT22] LOI: Khong doc duoc du lieu!");
  return false;
}

// a) HTTP GET – URL-encoded query string
void sendHTTPGet() {
  Serial.println("\n--- [a] HTTP GET (URL-encoded query string) ---");
  String url = String(URL_GET)
             + "?temperature=" + String(temperature, 1)
             + "&humidity="    + String(humidity, 1)
             + "&unit=metric&device=ESP32";
  Serial.println("[GET] URL: " + url);

  HTTPClient http;
  http.begin(url);
  int httpCode = http.GET();
  if (httpCode > 0) {
    Serial.println("[GET] HTTP Code: " + String(httpCode));
    if (httpCode == HTTP_CODE_OK)
      Serial.println(http.getString());
  } else {
    Serial.println("[GET] Loi: " + http.errorToString(httpCode));
  }
  http.end();
}

// b) HTTP POST – URL-encoded body
void sendHTTPPostUrlEncoded() {
  Serial.println("\n--- [b] HTTP POST (URL-encoded body) ---");
  String body = "temperature=" + String(temperature, 1)
              + "&humidity="   + String(humidity, 1)
              + "&unit=metric&device=ESP32";
  Serial.println("[POST/URL] Body: " + body);

  HTTPClient http;
  http.begin(URL_POST);
  http.addHeader("Content-Type", "application/x-www-form-urlencoded");
  int httpCode = http.POST(body);
  if (httpCode > 0) {
    Serial.println("[POST/URL] HTTP Code: " + String(httpCode));
    if (httpCode == HTTP_CODE_OK)
      Serial.println(http.getString());
  } else {
    Serial.println("[POST/URL] Loi: " + http.errorToString(httpCode));
  }
  http.end();
}

// c) HTTP POST – JSON body
void sendHTTPPostJSON() {
  Serial.println("\n--- [c] HTTP POST (JSON body) ---");
  StaticJsonDocument<256> jsonDoc;
  jsonDoc["device"]      = "ESP32";
  jsonDoc["temperature"] = serialized(String(temperature, 1));
  jsonDoc["humidity"]    = serialized(String(humidity, 1));
  jsonDoc["unit"]        = "metric";
  jsonDoc["timestamp"]   = millis();

  String jsonBody;
  serializeJson(jsonDoc, jsonBody);
  Serial.println("[POST/JSON] Body: " + jsonBody);

  HTTPClient http;
  http.begin(URL_POST);
  http.addHeader("Content-Type", "application/json");
  int httpCode = http.POST(jsonBody);
  if (httpCode > 0) {
    Serial.println("[POST/JSON] HTTP Code: " + String(httpCode));
    if (httpCode == HTTP_CODE_OK)
      Serial.println(http.getString());
  } else {
    Serial.println("[POST/JSON] Loi: " + http.errorToString(httpCode));
  }
  http.end();
}

void setup() {
  Serial.begin(115200);
  delay(1000);
  Serial.println("==============================================");
  Serial.println("  Bai 5: ESP32 Gui Du Lieu HTTP");
  Serial.println("  Mon: Kien truc & Bao mat IoT");
  Serial.println("==============================================");
  dhtSensor.setup(DHT_PIN, DHTesp::DHT22);
  connectToWiFi();
  Serial.println("[SYSTEM] He thong san sang!");
}

void loop() {
  if (WiFi.status() != WL_CONNECTED) connectToWiFi();

  unsigned long currentTime = millis();
  if (currentTime - lastSendTime >= SEND_INTERVAL_MS) {
    lastSendTime = currentTime;
    Serial.println("\n==============================================");
    Serial.println("[SEND] Bat dau chu ky gui du lieu...");
    if (readDHT22()) {
      sendHTTPGet();
      sendHTTPPostUrlEncoded();
      sendHTTPPostJSON();
    }
    Serial.println("[SEND] Hoan thanh! Cho 10 giay...");
    Serial.println("==============================================");
  }
}
```

### 3.2 File `diagram.json`

```json
{
  "version": 1,
  "editor": "wokwi",
  "parts": [
    { "type": "board-esp32-devkit-c-v4", "id": "esp", "top": 0, "left": 0 },
    { "type": "wokwi-dht22", "id": "dht1", "top": -9.3, "left": 230,
      "attrs": { "temperature": "28.5", "humidity": "65.0" } }
  ],
  "connections": [
    [ "esp:TX", "$serialMonitor:RX", "", [] ],
    [ "esp:RX", "$serialMonitor:TX", "", [] ],
    [ "dht1:SDA", "esp:15", "green", [] ],
    [ "dht1:VCC", "esp:3V3", "red", [] ],
    [ "dht1:GND", "esp:GND.2", "black", [] ]
  ]
}
```

### 3.3 File `libraries.txt`

```
ArduinoJson
DHT sensor library for ESPx
```

---

## 4. Giải thích hoạt động chương trình

### 4.1 Luồng xử lý

```
Khởi động
    ├─ Khởi tạo Serial (115200 baud)
    ├─ Khởi tạo DHT22 (GPIO 15)
    └─ Kết nối WiFi "Wokwi-GUEST"
           │
           ▼
        Loop (mỗi 10 giây)
           ├─ Đọc DHT22 → temperature, humidity
           ├─ [a] HTTP GET: postman-echo.com/get?temperature=28.5&humidity=65.0
           ├─ [b] HTTP POST URL-encoded: body=temperature=28.5&humidity=65.0
           └─ [c] HTTP POST JSON: {"device":"ESP32","temperature":"28.5",...}
```

### 4.2 So sánh 3 phương thức HTTP

| Tiêu chí | GET (URL-encoded) | POST (URL-encoded) | POST (JSON) |
|----------|------------------|-------------------|-------------|
| Vị trí dữ liệu | URL query string | Request body | Request body |
| Content-Type | - | application/x-www-form-urlencoded | application/json |
| Bảo mật | Thấp (lộ trong URL) | Trung bình | Trung bình |
| Dữ liệu phức tạp | Khó | Trung bình | Tốt nhất |
| REST API phổ biến | Read (CRUD) | Create/Update | Create/Update |

---

## 5. Link bài mô phỏng Wokwi

> 🔗 **Link Wokwi:** https://wokwi.com/projects/466454070666256385

**Hướng dẫn chạy mô phỏng:**
1. Truy cập link Wokwi ở trên
2. Nhấn nút **▶ Start Simulation** (màu xanh lá)
3. Mở tab **Serial Monitor** ở phía dưới màn hình
4. Quan sát output: ESP32 kết nối WiFi → đọc DHT22 → gửi 3 loại HTTP request

---

## 6. Kết quả mô phỏng

### 6.1 Sơ đồ mạch Wokwi

![Sơ đồ mạch ESP32 + DHT22](/home/duvx/.gemini/antigravity/brain/500e09a2-0777-499f-9acd-8694253e8ae4/wokwi_circuit_esp32_1781105325600.png)

*Hình 1: Sơ đồ mạch ESP32 DevKit C v4 kết nối với cảm biến DHT22 trên Wokwi*

### 6.2 Serial Monitor – Kết quả gửi HTTP

![Serial Monitor kết quả HTTP](/home/duvx/.gemini/antigravity/brain/500e09a2-0777-499f-9acd-8694253e8ae4/serial_monitor_http_1781105306483.png)

*Hình 2: Serial Monitor hiển thị kết quả gửi 3 loại HTTP request đến postman-echo.com*

### 6.3 Phản hồi mẫu từ postman-echo.com

**[a] GET Response:**
```json
{
  "args": {
    "device": "ESP32",
    "humidity": "65.0",
    "temperature": "28.5",
    "unit": "metric"
  },
  "url": "https://postman-echo.com/get?temperature=28.5&humidity=65.0&unit=metric&device=ESP32"
}
```

**[b] POST URL-encoded Response:**
```json
{
  "form": {
    "device": "ESP32",
    "humidity": "65.0",
    "temperature": "28.5",
    "unit": "metric"
  }
}
```

**[c] POST JSON Response:**
```json
{
  "json": {
    "device": "ESP32",
    "humidity": "65.0",
    "temperature": "28.5",
    "timestamp": 10023,
    "unit": "metric"
  }
}
```

---

## 7. Nhận xét và kết luận

### 7.1 Kết quả đạt được
- ✅ ESP32 kết nối WiFi thành công thông qua Wokwi-GUEST
- ✅ Đọc dữ liệu nhiệt độ (28.5°C) và độ ẩm (65.0%) từ cảm biến DHT22
- ✅ Gửi **HTTP GET** với dữ liệu đóng gói URL-encoded trong query string → Server phản hồi 200 OK
- ✅ Gửi **HTTP POST** với dữ liệu đóng gói URL-encoded trong request body → Server phản hồi 200 OK
- ✅ Gửi **HTTP POST** với dữ liệu đóng gói JSON trong request body → Server phản hồi 200 OK

### 7.2 Nhận xét
- **HTTP GET**: Đơn giản, phù hợp truy vấn dữ liệu; nhược điểm là dữ liệu lộ trên URL, giới hạn độ dài URL
- **HTTP POST URL-encoded**: Phù hợp với form submit truyền thống, dữ liệu nằm trong body nên an toàn hơn GET
- **HTTP POST JSON**: Linh hoạt nhất, phù hợp REST API hiện đại, hỗ trợ cấu trúc dữ liệu phức tạp (lồng nhau, mảng)

---

*Báo cáo thực hành môn Kiến trúc và Bảo mật cho ứng dụng IoT*  
*Học viện Bách khoa Hà Nội – Tháng 6, 2026*
