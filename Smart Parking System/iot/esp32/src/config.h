#ifndef CONFIG_H
#define CONFIG_H

// ======================================================================
// PRODUCTION_MODE is injected by platformio.ini [env:production]
// When NOT defined → Wokwi simulation mode (HiveMQ, no TLS)
// When     defined → Real hardware mode (Mosquitto, mTLS)
// ======================================================================

// --- WiFi Settings ---
#ifdef PRODUCTION_MODE
    // Injected from platformio.ini build_flags via -D REAL_WIFI_SSID=\"...\"
    #define WIFI_SSID       REAL_WIFI_SSID
    #define WIFI_PASSWORD   REAL_WIFI_PASSWORD
#else
    // Wokwi "magic" SSID — provides internet access inside the simulator
    #define WIFI_SSID       "Wokwi-GUEST"
    #define WIFI_PASSWORD   ""
#endif

// --- MQTT Settings ---
#ifdef PRODUCTION_MODE
    // Injected from platformio.ini build_flags via -D REAL_MQTT_HOST=\"...\"
    #define MQTT_HOST       REAL_MQTT_HOST
    #define MQTT_PORT       REAL_MQTT_PORT
    #define TOPIC_PREFIX    "parking"   // No prefix needed on private broker
#else
    // Public HiveMQ broker — free, no auth, no TLS (dev/simulation only)
    #define MQTT_HOST       "broker.hivemq.com"
    #define MQTT_PORT       1883
    // Unique prefix to avoid topic collision with other users on HiveMQ
    #define TOPIC_PREFIX    "smartpkg"
#endif

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

// Barrier Simulator (SG90 Servo)
#define SERVO_PIN       23

#endif // CONFIG_H
