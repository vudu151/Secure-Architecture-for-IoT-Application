package com.example.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONObject;
import org.json.JSONException;

public class MqttSubscriber implements MqttCallback {

    private final String brokerUrl;
    private final String clientId;
    private MqttClient client;
    private boolean isRunning = false;

    public MqttSubscriber(String brokerUrl, String clientId) {
        this.brokerUrl = brokerUrl;
        this.clientId = clientId;
    }

    /**
     * Connects to the MQTT broker and subscribes to the specified topic.
     *
     * @param topic The MQTT topic to listen to.
     */
    public synchronized void start(String topic) {
        if (isRunning) {
            System.out.println("[Subscriber] Already running.");
            return;
        }

        MemoryPersistence persistence = new MemoryPersistence();
        try {
            client = new MqttClient(brokerUrl, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setAutomaticReconnect(true); // Automatically reconnect if connection is lost
            connOpts.setConnectionTimeout(10);

            client.setCallback(this);

            System.out.println("[Subscriber] Connecting to broker: " + brokerUrl);
            client.connect(connOpts);
            System.out.println("[Subscriber] Connected successfully!");

            System.out.println("[Subscriber] Subscribing to topic: " + topic);
            client.subscribe(topic);
            isRunning = true;
            System.out.println("[Subscriber] Subscribed and waiting for messages...");

        } catch (MqttException me) {
            System.err.println("[Subscriber] Failed to start: " + me.getMessage());
            me.printStackTrace();
        }
    }

    /**
     * Unsubscribes and disconnects the MQTT client.
     */
    public synchronized void stop() {
        if (!isRunning) {
            System.out.println("[Subscriber] Subscriber is not running.");
            return;
        }

        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
                System.out.println("[Subscriber] Disconnected from broker.");
            }
        } catch (MqttException me) {
            System.err.println("[Subscriber] Error while stopping: " + me.getMessage());
        } finally {
            isRunning = false;
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    // --- MqttCallback Implementation ---

    @Override
    public void connectionLost(Throwable cause) {
        System.err.println("[Subscriber] Connection lost! Cause: " + (cause != null ? cause.getMessage() : "Unknown"));
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payloadString = new String(message.getPayload());
        System.out.println("\n-------------------------------------------");
        System.out.println("[Subscriber] New Message Received!");
        System.out.println("[Subscriber] Topic: " + topic);
        System.out.println("[Subscriber] Raw Payload: " + payloadString);

        try {
            // Parse payload as JSON
            JSONObject json = new JSONObject(payloadString);
            
            // Extract fields
            String deviceName = json.optString("DeviceName", "Unknown Device");
            double temperature = json.optDouble("temperature", Double.NaN);
            double humidity = json.optDouble("humidity", Double.NaN);

            // Display values
            System.out.println("[Subscriber] Parsed Fields:");
            System.out.println("  - Tên thiết bị (DeviceName) : " + deviceName);
            System.out.println("  - Nhiệt độ (Temperature)     : " + (Double.isNaN(temperature) ? "N/A" : temperature + " °C"));
            System.out.println("  - Độ ẩm (Humidity)           : " + (Double.isNaN(humidity) ? "N/A" : humidity + " %"));
            
        } catch (JSONException je) {
            System.err.println("[Subscriber] Error parsing JSON payload: " + je.getMessage());
        }
        System.out.println("-------------------------------------------");
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Not used for subscriber
    }
}
