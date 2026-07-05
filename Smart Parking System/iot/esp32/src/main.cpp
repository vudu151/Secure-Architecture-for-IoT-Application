#include <Arduino.h>
#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>
#include "certificates.h"
#include "config.h"

// --- Global Variables ---
WiFiClientSecure secureClient;
PubSubClient mqttClient(secureClient);

// Servo control variables using ESP32 LEDC (PWM)
const int servoChannel = 0;
const int servoFreq = 50;       // 50Hz frequency
const int servoResolution = 12; // 12-bit resolution (0-4095)
// Duty cycle mapping for SG90 servo:
// 0 degree (closed)  -> ~0.5ms pulse -> (0.5ms / 20ms) * 4096 = 102
// 90 degree (opened) -> ~1.5ms pulse -> (1.5ms / 20ms) * 4096 = 307
const int SERVO_CLOSED_DUTY = 102;
const int SERVO_OPENED_DUTY = 307;

// Debounce state variables
bool currentOccupiedState = false;
bool lastMeasuredOccupied = false;
unsigned long stateChangeTime = 0;
const unsigned long DEBOUNCE_DELAY_MS = 3000; // 3 seconds stable state required

// Timers
unsigned long lastHeartbeatTime = 0;
unsigned long lastSensorReadTime = 0;
const unsigned long HEARTBEAT_INTERVAL_MS = 60000; // 60 seconds
const unsigned long SENSOR_READ_INTERVAL_MS = 1000; // 1 second

// Barrier variables
bool isBarrierOpen = false;
unsigned long barrierOpenTime = 0;
const unsigned long BARRIER_AUTO_CLOSE_MS = 10000; // 10 seconds auto-close

// --- Helper Functions ---

void writeServo(int duty) {
    ledcWrite(servoChannel, duty);
}

void openBarrier() {
    Serial.println("[Barrier] Opening barrier (Servo to 90 deg)...");
    writeServo(SERVO_OPENED_DUTY);
    isBarrierOpen = true;
    barrierOpenTime = millis();
}

void closeBarrier() {
    Serial.println("[Barrier] Closing barrier (Servo to 0 deg)...");
    writeServo(SERVO_CLOSED_DUTY);
    isBarrierOpen = false;
}

void setupWiFi() {
    delay(10);
    Serial.println();
    Serial.print("Connecting to WiFi SSID: ");
    Serial.println(WIFI_SSID);

    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

    while (WiFi.status() != WL_CONNECTED) {
        delay(500);
        Serial.print(".");
    }

    Serial.println("");
    Serial.println("WiFi connected successfully!");
    Serial.print("IP address: ");
    Serial.println(WiFi.localIP());
}

void publishSlotStatus(bool occupied) {
    char topic[60];
    sprintf(topic, "parking/slots/%s/status", SLOT_CODE);

    JsonDocument doc;
    doc["slotCode"] = SLOT_CODE;
    doc["occupied"] = occupied;
    doc["timestamp"] = millis(); // Local boot timestamp

    char payload[128];
    serializeJson(doc, payload);

    Serial.print("Publishing slot status: ");
    Serial.println(payload);
    
    if (mqttClient.publish(topic, payload, true)) {
        Serial.println("Slot status published successfully.");
    } else {
        Serial.println("Failed to publish slot status.");
    }
}

void sendHeartbeat() {
    char topic[80];
    sprintf(topic, "parking/devices/esp32_slot_%s/heartbeat", SLOT_CODE);

    JsonDocument doc;
    doc["deviceUid"] = String("esp32_slot_") + SLOT_CODE;
    doc["firmwareVersion"] = FIRMWARE_VER;
    doc["uptime"] = millis();

    char payload[128];
    serializeJson(doc, payload);

    Serial.print("Sending heartbeat: ");
    Serial.println(payload);
    mqttClient.publish(topic, payload);
}

void mqttCallback(char* topic, byte* payload, unsigned int length) {
    Serial.print("Message arrived [");
    Serial.print(topic);
    Serial.print("]: ");
    
    String message = "";
    for (unsigned int i = 0; i < length; i++) {
        message += (char)payload[i];
    }
    Serial.println(message);

    JsonDocument doc;
    DeserializationError error = deserializeJson(doc, message);
    if (error) {
        Serial.print("JSON deserialization failed: ");
        Serial.println(error.c_str());
        return;
    }

    String topicStr = String(topic);
    char slotCmdTopic[60];
    sprintf(slotCmdTopic, "parking/slots/%s/command", SLOT_CODE);
    char gateCtrlTopic[60];
    sprintf(gateCtrlTopic, "parking/gates/%s/control", GATE_ID);

    if (topicStr == slotCmdTopic) {
        // LED commands: {"color": "RED"/"GREEN"/"YELLOW"}
        String color = doc["color"];
        Serial.print("LED Command received: ");
        Serial.println(color);
        if (color == "GREEN") {
            digitalWrite(LED_GREEN_PIN, HIGH);
            digitalWrite(LED_RED_PIN, LOW);
            digitalWrite(LED_YELLOW_PIN, LOW);
        } else if (color == "RED") {
            digitalWrite(LED_GREEN_PIN, LOW);
            digitalWrite(LED_RED_PIN, HIGH);
            digitalWrite(LED_YELLOW_PIN, LOW);
        } else if (color == "YELLOW") {
            digitalWrite(LED_GREEN_PIN, LOW);
            digitalWrite(LED_RED_PIN, LOW);
            digitalWrite(LED_YELLOW_PIN, HIGH);
        }
    } else if (topicStr == gateCtrlTopic) {
        // Gate control commands: {"action": "OPEN"/"CLOSE"}
        String action = doc["action"];
        Serial.print("Gate control action: ");
        Serial.println(action);
        if (action == "OPEN") {
            openBarrier();
        } else if (action == "CLOSE") {
            closeBarrier();
        }
    }
}

void reconnectMQTT() {
    while (!mqttClient.connected()) {
        Serial.print("Attempting MQTT connection to: ");
        Serial.print(MQTT_HOST);
        Serial.print(":");
        Serial.println(MQTT_PORT);
        
        // Connect to MQTT. Username/Password are not needed since Mosquitto validates certificates via mTLS
        // We use the slot code as client ID.
        char client_id[30];
        sprintf(client_id, "esp32_slot_%s", SLOT_CODE);
        
        if (mqttClient.connect(client_id)) {
            Serial.println("Connected to Mosquitto Broker successfully via mTLS!");
            
            // Subscribe to LED command topic
            char slotCmdTopic[60];
            sprintf(slotCmdTopic, "parking/slots/%s/command", SLOT_CODE);
            mqttClient.subscribe(slotCmdTopic, 1);
            Serial.print("Subscribed to: ");
            Serial.println(slotCmdTopic);

            // Subscribe to Gate control topic
            char gateCtrlTopic[60];
            sprintf(gateCtrlTopic, "parking/gates/%s/control", GATE_ID);
            mqttClient.subscribe(gateCtrlTopic, 1);
            Serial.print("Subscribed to: ");
            Serial.println(gateCtrlTopic);

            // Publish initial status on reconnection
            publishSlotStatus(currentOccupiedState);
            sendHeartbeat();
        } else {
            Serial.print("MQTT Connection failed, state=");
            Serial.print(mqttClient.state());
            Serial.println(". Retrying in 5 seconds...");
            delay(5000);
        }
    }
}

float measureDistanceCm() {
    // Send 10 microsecond trigger pulse
    digitalWrite(TRIG_PIN, LOW);
    delayMicroseconds(2);
    digitalWrite(TRIG_PIN, HIGH);
    delayMicroseconds(10);
    digitalWrite(TRIG_PIN, LOW);

    // Read echo travel time in microseconds
    long duration = pulseIn(ECHO_PIN, HIGH, 30000); // 30ms timeout
    if (duration == 0) {
        return 999.0; // Out of range or no obstacle
    }
    
    // Calculate distance (speed of sound is ~343 m/s or 0.0343 cm/us)
    float distance = (duration * 0.0343) / 2;
    return distance;
}

// --- Setup & Loop ---

void setup() {
    Serial.begin(115200);
    
    // Config pin modes
    pinMode(TRIG_PIN, OUTPUT);
    pinMode(ECHO_PIN, INPUT);
    pinMode(LED_GREEN_PIN, OUTPUT);
    pinMode(LED_RED_PIN, OUTPUT);
    pinMode(LED_YELLOW_PIN, OUTPUT);

    // Initial LED State: Green on (trống)
    digitalWrite(LED_GREEN_PIN, HIGH);
    digitalWrite(LED_RED_PIN, LOW);
    digitalWrite(LED_YELLOW_PIN, LOW);

    // Config LEDC for Servo Control
    ledcSetup(servoChannel, servoFreq, servoResolution);
    ledcAttachPin(SERVO_PIN, servoChannel);
    writeServo(SERVO_CLOSED_DUTY); // Initialize barrier to closed

    setupWiFi();

    // Configure client certificates for mTLS connection
    secureClient.setCACert(ca_cert);
    secureClient.setCertificate(client_cert);
    secureClient.setPrivateKey(client_key);

    mqttClient.setServer(MQTT_HOST, MQTT_PORT);
    mqttClient.setCallback(mqttCallback);
}

void loop() {
    if (WiFi.status() != WL_CONNECTED) {
        setupWiFi();
    }
    
    if (!mqttClient.connected()) {
        reconnectMQTT();
    }
    
    mqttClient.loop();

    unsigned long currentMillis = millis();

    // 1. Periodic Sensor Reading (every 1 second)
    if (currentMillis - lastSensorReadTime >= SENSOR_READ_INTERVAL_MS) {
        lastSensorReadTime = currentMillis;
        float distance = measureDistanceCm();
        
        Serial.print("[Sensor] Measured Distance: ");
        Serial.print(distance);
        Serial.println(" cm");

        // occupied threshold: < 30 cm
        bool isOccupied = (distance < 30.0);

        if (isOccupied != lastMeasuredOccupied) {
            // State changed, record time
            lastMeasuredOccupied = isOccupied;
            stateChangeTime = currentMillis;
        } else if (isOccupied != currentOccupiedState) {
            // State stable at different state, check debounce time
            if (currentMillis - stateChangeTime >= DEBOUNCE_DELAY_MS) {
                currentOccupiedState = isOccupied;
                publishSlotStatus(currentOccupiedState);
                
                // Update local LEDs instantly for feedback
                if (currentOccupiedState) {
                    digitalWrite(LED_GREEN_PIN, LOW);
                    digitalWrite(LED_RED_PIN, HIGH);
                    digitalWrite(LED_YELLOW_PIN, LOW);
                } else {
                    digitalWrite(LED_GREEN_PIN, HIGH);
                    digitalWrite(LED_RED_PIN, LOW);
                    digitalWrite(LED_YELLOW_PIN, LOW);
                }
            }
        }
    }

    // 2. Auto-close Barrier (after 10 seconds)
    if (isBarrierOpen && (currentMillis - barrierOpenTime >= BARRIER_AUTO_CLOSE_MS)) {
        closeBarrier();
    }

    // 3. Periodic Heartbeat (every 60 seconds)
    if (currentMillis - lastHeartbeatTime >= HEARTBEAT_INTERVAL_MS) {
        lastHeartbeatTime = currentMillis;
        sendHeartbeat();
    }
}
