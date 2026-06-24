package com.example.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONObject;

public class MqttPublisher {

    private final String brokerUrl;
    private final String clientId;

    public MqttPublisher(String brokerUrl, String clientId) {
        this.brokerUrl = brokerUrl;
        this.clientId = clientId;
    }

    /**
     * Publishes sensor data to a specific MQTT topic as a JSON string.
     *
     * @param topic       The MQTT topic to publish to.
     * @param deviceName  The name of the device.
     * @param temperature The temperature reading.
     * @param humidity    The humidity reading.
     */
    public void publishData(String topic, String deviceName, double temperature, double humidity) {
        MemoryPersistence persistence = new MemoryPersistence();
        try {
            // Instantiate MqttClient
            MqttClient client = new MqttClient(brokerUrl, clientId, persistence);
            
            // Set Connection Options
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setConnectionTimeout(10);
            
            System.out.println("[Publisher] Connecting to broker: " + brokerUrl);
            client.connect(connOpts);
            System.out.println("[Publisher] Connected successfully!");

            // Prepare JSON Payload
            JSONObject payload = new JSONObject();
            payload.put("DeviceName", deviceName);
            payload.put("temperature", temperature);
            payload.put("humidity", humidity);
            
            String payloadString = payload.toString(4); // Pretty print with 4 indentation spaces
            
            // Create MqttMessage
            MqttMessage message = new MqttMessage(payloadString.getBytes());
            message.setQos(1); // At least once delivery
            
            System.out.println("[Publisher] Publishing message to topic: " + topic);
            System.out.println("[Publisher] Payload:\n" + payloadString);
            
            client.publish(topic, message);
            System.out.println("[Publisher] Message published!");
            
            // Disconnect client
            client.disconnect();
            System.out.println("[Publisher] Disconnected from broker.");
            
        } catch (MqttException me) {
            System.err.println("[Publisher] MQTT Error details:");
            System.err.println("reason " + me.getReasonCode());
            System.err.println("msg " + me.getMessage());
            System.err.println("loc " + me.getLocalizedMessage());
            System.err.println("cause " + me.getCause());
            System.err.println("excep " + me);
            me.printStackTrace();
        } catch (Exception e) {
            System.err.println("[Publisher] General Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
