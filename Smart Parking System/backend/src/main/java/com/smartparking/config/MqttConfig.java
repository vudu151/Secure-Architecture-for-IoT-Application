package com.smartparking.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartparking.enums.BookingStatus;
import com.smartparking.service.DeviceService;
import com.smartparking.service.ParkingSlotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.Map;

@Configuration
@Slf4j
public class MqttConfig {

    @Value("${mqtt.broker.host:localhost}")
    private String host;

    @Value("${mqtt.broker.port:1883}")
    private int port;

    @Value("${mqtt.client.id:smart-parking-backend}")
    private String clientId;

    @Value("${mqtt.broker.username:}")
    private String username;

    @Value("${mqtt.broker.password:}")
    private String password;

    @Bean
    public MqttClient mqttClient(
            @Lazy ParkingSlotService parkingSlotService,
            @Lazy DeviceService deviceService,
            ObjectMapper objectMapper
    ) throws MqttException {
        String serverURI = "tcp://" + host + ":" + port;
        MqttClient client = new MqttClient(serverURI, clientId, new MemoryPersistence());
        
        MqttConnectOptions options = new MqttConnectOptions();
        if (username != null && !username.isEmpty()) {
            options.setUserName(username);
            options.setPassword(password.toCharArray());
        }
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(60);

        client.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                log.info("MQTT Client connected to broker at {}. Reconnect={}", serverURI, reconnect);
                try {
                    client.subscribe("parking/slots/+/status", 1);
                    client.subscribe("parking/gates/+/event", 1);
                    client.subscribe("parking/devices/+/heartbeat", 1);
                    log.info("Subscribed to MQTT topics successfully");
                } catch (MqttException e) {
                    log.error("Failed to subscribe to MQTT topics: {}", e.getMessage());
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                log.warn("MQTT Connection lost: {}", cause != null ? cause.getMessage() : "Unknown reason");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                String payload = new String(message.getPayload());
                log.debug("MQTT message arrived on topic: {}, payload: {}", topic, payload);
                
                try {
                    String[] topicParts = topic.split("/");
                    
                    if (topic.startsWith("parking/slots/") && topic.endsWith("/status")) {
                        // Topic: parking/slots/{slotCode}/status
                        String slotCode = topicParts[2];
                        Map<?, ?> map = objectMapper.readValue(payload, Map.class);
                        Boolean occupied = (Boolean) map.get("occupied");
                        if (occupied != null) {
                            parkingSlotService.updateSlotStatus(slotCode, occupied);
                        }
                    } else if (topic.startsWith("parking/gates/") && topic.endsWith("/event")) {
                        // Topic: parking/gates/{gateId}/event
                        String gateId = topicParts[2];
                        Map<?, ?> map = objectMapper.readValue(payload, Map.class);
                        String eventType = (String) map.get("event");
                        
                        if ("PLATE_DETECTED".equalsIgnoreCase(eventType)) {
                            String plate = (String) map.get("plate");
                            if (plate != null) {
                                deviceService.verifyPlate(plate, gateId);
                            }
                        } else if ("QR_SCANNED".equalsIgnoreCase(eventType)) {
                            String qrData = (String) map.get("qrData");
                            if (qrData != null) {
                                deviceService.verifyQr(qrData, gateId);
                            }
                        }
                    } else if (topic.startsWith("parking/devices/") && topic.endsWith("/heartbeat")) {
                        // Topic: parking/devices/{deviceUid}/heartbeat
                        String deviceUid = topicParts[2];
                        Map<?, ?> map = objectMapper.readValue(payload, Map.class);
                        String version = (String) map.get("firmwareVersion");
                        deviceService.registerHeartbeat(deviceUid, version);
                    }
                } catch (Exception e) {
                    log.error("Error processing MQTT message on topic {}: {}", topic, e.getMessage());
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // Sent message delivery complete
            }
        });

        try {
            client.connect(options);
        } catch (MqttException e) {
            log.error("Failed to connect to MQTT broker initially: {}", e.getMessage());
        }

        return client;
    }
}
