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
 *
 * Phần cứng sử dụng:
 *   - Board   : ESP32 DevKit C v4
 *   - Sensor  : DHT22 (kết nối chân GPIO 15)
 *   - WiFi    : tích hợp sẵn trên ESP32 (mô phỏng bằng Wokwi)
 *
 * Thư viện sử dụng:
 *   - WiFi.h        : kết nối WiFi (built-in ESP32)
 *   - HTTPClient.h  : gửi HTTP request (built-in ESP32)
 *   - ArduinoJson   : tạo JSON body cho POST request
 *   - DHTesp        : đọc dữ liệu DHT22 trên ESP32
 * =====================================================================
 */

#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include "DHTesp.h"

// =====================================================================
// Cấu hình WiFi (Wokwi Simulator dùng SSID = "Wokwi-GUEST")
// =====================================================================
const char* WIFI_SSID     = "Wokwi-GUEST";
const char* WIFI_PASSWORD = "";

// =====================================================================
// URL server postman-echo
// =====================================================================
const char* URL_GET  = "https://postman-echo.com/get";
const char* URL_POST = "https://postman-echo.com/post";

// =====================================================================
// Cấu hình cảm biến DHT22
// =====================================================================
#define DHT_PIN          15      // Chân DATA của DHT22 → GPIO 15
#define SEND_INTERVAL_MS 10000  // Chu kỳ gửi dữ liệu: 10 giây

// =====================================================================
// Khởi tạo đối tượng cảm biến
// =====================================================================
DHTesp dhtSensor;

// =====================================================================
// Biến toàn cục
// =====================================================================
unsigned long lastSendTime = 0;
float temperature = 0.0;
float humidity    = 0.0;

// =====================================================================
// Hàm kết nối WiFi
// =====================================================================
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
    Serial.println("\n[WiFi] Ket noi that bai. Dang thu lai...");
  }
}

// =====================================================================
// Hàm đọc dữ liệu cảm biến DHT22
// =====================================================================
bool readDHT22() {
  TempAndHumidity data = dhtSensor.getTempAndHumidity();
  if (!isnan(data.temperature) && !isnan(data.humidity)) {
    temperature = data.temperature;
    humidity    = data.humidity;
    Serial.println("[DHT22] Nhiet do: " + String(temperature, 1) + " C");
    Serial.println("[DHT22] Do am:    " + String(humidity, 1) + " %");
    return true;
  }
  Serial.println("[DHT22] LOI: Khong doc duoc du lieu cam bien!");
  return false;
}

// =====================================================================
// a) HTTP GET Request – dữ liệu URL-encoded trong query string
//    URL: https://postman-echo.com/get?temperature=XX.X&humidity=XX.X
// =====================================================================
void sendHTTPGet() {
  Serial.println("\n--- [a] HTTP GET Request (URL-encoded) ---");

  // Xây dựng URL với query string
  String url = String(URL_GET)
             + "?temperature=" + String(temperature, 1)
             + "&humidity="    + String(humidity, 1)
             + "&unit=metric"
             + "&device=ESP32";

  Serial.println("[GET] URL: " + url);

  HTTPClient http;
  http.begin(url);
  http.addHeader("Content-Type", "application/x-www-form-urlencoded");

  int httpCode = http.GET();

  if (httpCode > 0) {
    Serial.println("[GET] Ma phan hoi HTTP: " + String(httpCode));
    if (httpCode == HTTP_CODE_OK) {
      String response = http.getString();
      Serial.println("[GET] Phan hoi tu server:");
      Serial.println(response);
    }
  } else {
    Serial.println("[GET] Loi HTTP: " + http.errorToString(httpCode));
  }

  http.end();
}

// =====================================================================
// b) HTTP POST Request – dữ liệu URL-encoded trong request body
//    Content-Type: application/x-www-form-urlencoded
// =====================================================================
void sendHTTPPostUrlEncoded() {
  Serial.println("\n--- [b] HTTP POST Request (URL-encoded body) ---");

  // Tạo body dạng URL-encoded
  String postBody = "temperature=" + String(temperature, 1)
                  + "&humidity="   + String(humidity, 1)
                  + "&unit=metric"
                  + "&device=ESP32";

  Serial.println("[POST/URL] Body: " + postBody);

  HTTPClient http;
  http.begin(URL_POST);
  http.addHeader("Content-Type", "application/x-www-form-urlencoded");

  int httpCode = http.POST(postBody);

  if (httpCode > 0) {
    Serial.println("[POST/URL] Ma phan hoi HTTP: " + String(httpCode));
    if (httpCode == HTTP_CODE_OK) {
      String response = http.getString();
      Serial.println("[POST/URL] Phan hoi tu server:");
      Serial.println(response);
    }
  } else {
    Serial.println("[POST/URL] Loi HTTP: " + http.errorToString(httpCode));
  }

  http.end();
}

// =====================================================================
// c) HTTP POST Request – dữ liệu JSON trong request body
//    Content-Type: application/json
// =====================================================================
void sendHTTPPostJSON() {
  Serial.println("\n--- [c] HTTP POST Request (JSON body) ---");

  // Tạo JSON document
  StaticJsonDocument<256> jsonDoc;
  jsonDoc["device"]      = "ESP32";
  jsonDoc["temperature"] = serialized(String(temperature, 1));
  jsonDoc["humidity"]    = serialized(String(humidity, 1));
  jsonDoc["unit"]        = "metric";
  jsonDoc["timestamp"]   = millis();

  // Chuyển JSON thành string
  String jsonBody;
  serializeJson(jsonDoc, jsonBody);

  Serial.println("[POST/JSON] Body: " + jsonBody);

  HTTPClient http;
  http.begin(URL_POST);
  http.addHeader("Content-Type", "application/json");

  int httpCode = http.POST(jsonBody);

  if (httpCode > 0) {
    Serial.println("[POST/JSON] Ma phan hoi HTTP: " + String(httpCode));
    if (httpCode == HTTP_CODE_OK) {
      String response = http.getString();
      Serial.println("[POST/JSON] Phan hoi tu server:");
      Serial.println(response);
    }
  } else {
    Serial.println("[POST/JSON] Loi HTTP: " + http.errorToString(httpCode));
  }

  http.end();
}

// =====================================================================
// Hàm setup() – chạy một lần khi khởi động
// =====================================================================
void setup() {
  Serial.begin(115200);
  delay(1000);

  Serial.println("==============================================");
  Serial.println("  Bai 5: ESP32 Gui Du Lieu HTTP");
  Serial.println("  Mon: Kien truc & Bao mat IoT");
  Serial.println("==============================================");

  // Khởi động cảm biến DHT22
  dhtSensor.setup(DHT_PIN, DHTesp::DHT22);
  Serial.println("[DHT22] Cam bien da duoc khoi dong.");

  // Kết nối WiFi
  connectToWiFi();

  Serial.println("\n[SYSTEM] He thong san sang! Bat dau gui du lieu...");
  Serial.println("----------------------------------------------");
}

// =====================================================================
// Hàm loop() – lặp liên tục
// =====================================================================
void loop() {
  // Kiểm tra kết nối WiFi, tự động kết nối lại nếu mất
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("[WiFi] Mat ket noi. Dang ket noi lai...");
    connectToWiFi();
  }

  unsigned long currentTime = millis();

  // Gửi dữ liệu theo chu kỳ SEND_INTERVAL_MS (10s)
  if (currentTime - lastSendTime >= SEND_INTERVAL_MS) {
    lastSendTime = currentTime;

    Serial.println("\n==============================================");
    Serial.println("[SEND] Bat dau chu ky gui du lieu moi...");

    // Đọc dữ liệu cảm biến
    if (readDHT22()) {
      // Gửi 3 loại HTTP request
      sendHTTPGet();           // a) GET với URL-encoded query string
      sendHTTPPostUrlEncoded(); // b) POST với URL-encoded body
      sendHTTPPostJSON();       // c) POST với JSON body
    }

    Serial.println("\n[SEND] Hoan thanh! Cho " + String(SEND_INTERVAL_MS/1000) + " giay...");
    Serial.println("==============================================");
  }
}
