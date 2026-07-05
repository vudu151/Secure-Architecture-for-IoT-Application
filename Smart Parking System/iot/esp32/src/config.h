#ifndef CONFIG_H
#define CONFIG_H

// --- WiFi Settings ---
#define WIFI_SSID       "Your_WiFi_SSID"
#define WIFI_PASSWORD   "Your_WiFi_Password"

// --- MQTT Settings ---
// Note: Use your computer's local IP address (e.g. 192.168.1.X) where Docker is running.
// Do NOT use "localhost" or "127.0.0.1" as it refers to the ESP32 itself.
#define MQTT_HOST       "192.168.1.100" 
#define MQTT_PORT       8883 // mTLS Port

// --- Device Configuration ---
#define SLOT_CODE       "A01"
#define GATE_ID         "gate1"
#define FIRMWARE_VER    "1.0.0"

// --- Pin Configurations (ESP32 DevKit V1) ---
// Ultrasonic Sensor HC-SR04
#define TRIG_PIN        5
#define ECHO_PIN        18

// Status Indicator LEDs
#define LED_GREEN_PIN   19  // Available
#define LED_RED_PIN     21  // Occupied
#define LED_YELLOW_PIN  22  // Reserved

// Barrier Simulator (SG90 Servo)
#define SERVO_PIN       23

#endif // CONFIG_H
