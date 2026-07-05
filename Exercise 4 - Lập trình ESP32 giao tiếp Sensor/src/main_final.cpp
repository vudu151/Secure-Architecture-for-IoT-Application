/**
 * =====================================================================
 * Bài 4: Lập trình ESP32 thu thập dữ liệu sensor
 * Môn học: Kiến trúc và Bảo mật cho ứng dụng IoT
 * =====================================================================
 * Mô tả chương trình:
 *   - Đọc dữ liệu nhiệt độ và độ ẩm từ cảm biến DHT22 mỗi 2 giây
 *   - Hiển thị nhiệt độ, độ ẩm lên màn hình LCD 16x2 qua giao tiếp I2C
 *   - Phát hiện chuyển động qua cảm biến PIR:
 *       + Có chuyển động → bật LED
 *       + Không có chuyển động → tắt LED
 *   - Gửi log dữ liệu qua Serial (115200 baud)
 *
 * Phần cứng sử dụng:
 *   - Board: ESP32 DevKit C v4
 *   - Cảm biến nhiệt độ/độ ẩm: DHT22 (kết nối chân GPIO 15)
 *   - Màn hình LCD 16x2 giao tiếp I2C (SDA=GPIO21, SCL=GPIO22, địa chỉ 0x27)
 *   - Cảm biến chuyển động PIR (kết nối chân GPIO 12)
 *   - LED đỏ (kết nối chân GPIO 32 qua điện trở 1kΩ)
 *
 * Thư viện sử dụng:
 *   - DHTesp: đọc dữ liệu từ cảm biến DHT22
 *   - LiquidCrystal_I2C: điều khiển màn hình LCD I2C
 * =====================================================================
 */

#include "DHTesp.h"           // Thư viện giao tiếp cảm biến DHT cho ESP32
#include "LiquidCrystal_I2C.h" // Thư viện điều khiển LCD qua giao tiếp I2C

// =====================================================================
// Định nghĩa chân kết nối (PIN definitions)
// =====================================================================
#define DHT_PIN     15   // Chân DATA của DHT22 kết nối với GPIO 15
#define PIR_PIN     12   // Chân OUT của PIR sensor kết nối với GPIO 12
#define LED_PIN     32   // Chân điều khiển LED kết nối với GPIO 32

// =====================================================================
// Định nghĩa thời gian đọc cảm biến
// =====================================================================
#define READ_INTERVAL_MS  2000  // Đọc dữ liệu DHT22 mỗi 2000ms (2 giây)

// =====================================================================
// Khởi tạo đối tượng cảm biến và LCD
// =====================================================================
DHTesp dhtSensor;  // Đối tượng đọc dữ liệu DHT22

/**
 * Khởi tạo LCD I2C:
 *   - Địa chỉ I2C: 0x27
 *   - Số cột: 16
 *   - Số hàng: 2
 */
LiquidCrystal_I2C lcd(0x27, 16, 2);

// =====================================================================
// Biến toàn cục
// =====================================================================
unsigned long lastReadTime = 0;   // Thời điểm đọc cảm biến DHT22 lần trước
float currentTemp   = 0.0;        // Nhiệt độ hiện tại (°C)
float currentHumid  = 0.0;        // Độ ẩm hiện tại (%)
bool motionDetected = false;      // Trạng thái phát hiện chuyển động

// =====================================================================
// Hàm setup() - Chạy một lần khi khởi động
// =====================================================================
void setup() {
  // --- Khởi động Serial Monitor ---
  Serial.begin(115200);
  Serial.println("===========================================");
  Serial.println("  ESP32 - Thu thap du lieu Sensor");
  Serial.println("===========================================");

  // --- Cấu hình chân GPIO ---
  pinMode(LED_PIN, OUTPUT);     // GPIO 32 → đầu ra (điều khiển LED)
  pinMode(PIR_PIN, INPUT);      // GPIO 12 → đầu vào (đọc tín hiệu PIR)
  digitalWrite(LED_PIN, LOW);   // Tắt LED khi khởi động

  // --- Khởi động cảm biến DHT22 ---
  dhtSensor.setup(DHT_PIN, DHTesp::DHT22);
  Serial.println("[DHT22] Cam bien nhiet do/do am da san sang.");

  // --- Khởi động LCD ---
  lcd.init();           // Khởi tạo LCD
  lcd.backlight();      // Bật đèn nền LCD

  // Hiển thị thông báo khởi động trên LCD
  lcd.setCursor(0, 0);
  lcd.print("  ESP32 Sensor  ");
  lcd.setCursor(0, 1);
  lcd.print("  Initializing..");
  Serial.println("[LCD] Man hinh da san sang.");

  delay(2000); // Hiển thị thông báo khởi động trong 2 giây
  lcd.clear();

  Serial.println("[SYSTEM] He thong san sang!");
  Serial.println("-------------------------------------------");
}

// =====================================================================
// Hàm đọc và cập nhật dữ liệu DHT22
// =====================================================================
void readDHTSensor() {
  TempAndHumidity data = dhtSensor.getTempAndHumidity();

  // Kiểm tra dữ liệu hợp lệ (DHTesp trả về NaN nếu đọc lỗi)
  if (!isnan(data.temperature) && !isnan(data.humidity)) {
    currentTemp  = data.temperature;
    currentHumid = data.humidity;

    // Ghi log ra Serial
    Serial.println("[DHT22] Nhiet do: " + String(currentTemp, 1) + " C");
    Serial.println("[DHT22] Do am:    " + String(currentHumid, 1) + " %");
  } else {
    Serial.println("[DHT22] LOI: Khong doc duoc du lieu cam bien!");
  }
}

// =====================================================================
// Hàm cập nhật màn hình LCD
// =====================================================================
void updateLCD() {
  // Dòng 1: Hiển thị nhiệt độ
  lcd.setCursor(0, 0);
  lcd.print("Temp: ");
  lcd.print(String(currentTemp, 1));
  lcd.print((char)223);  // Ký tự độ '°'
  lcd.print("C   ");     // Xóa ký tự thừa từ lần in trước

  // Dòng 2: Hiển thị độ ẩm
  lcd.setCursor(0, 1);
  lcd.print("Humid:");
  lcd.print(String(currentHumid, 1));
  lcd.print("%   ");     // Xóa ký tự thừa
}

// =====================================================================
// Hàm xử lý cảm biến PIR và điều khiển LED
// =====================================================================
void handlePIRSensor() {
  int pirValue = digitalRead(PIR_PIN); // Đọc tín hiệu từ PIR (HIGH=có chuyển động)

  if (pirValue == HIGH) {
    // Phát hiện chuyển động
    if (!motionDetected) {
      // Chỉ in log khi trạng thái thay đổi (tránh spam)
      Serial.println("[PIR] *** Phat hien chuyen dong! LED ON ***");
      motionDetected = true;
    }
    digitalWrite(LED_PIN, HIGH);  // Bật LED
  } else {
    // Không có chuyển động
    if (motionDetected) {
      // Chỉ in log khi trạng thái thay đổi
      Serial.println("[PIR] Khong co chuyen dong. LED OFF.");
      motionDetected = false;
    }
    digitalWrite(LED_PIN, LOW);   // Tắt LED
  }
}

// =====================================================================
// Hàm loop() - Lặp liên tục
// =====================================================================
void loop() {
  unsigned long currentTime = millis(); // Thời gian hiện tại (ms)

  // --- Đọc DHT22 và cập nhật LCD theo chu kỳ READ_INTERVAL_MS (2s) ---
  if (currentTime - lastReadTime >= READ_INTERVAL_MS) {
    lastReadTime = currentTime;

    readDHTSensor();  // Đọc dữ liệu nhiệt độ/độ ẩm
    updateLCD();      // Cập nhật màn hình LCD
    Serial.println("-------------------------------------------");
  }

  // --- Xử lý PIR sensor liên tục (không bị chặn bởi delay) ---
  handlePIRSensor();

  delay(100); // Tránh đọc PIR quá nhanh, giảm nhiễu
}
