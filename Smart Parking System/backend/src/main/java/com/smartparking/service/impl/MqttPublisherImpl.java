package com.smartparking.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartparking.service.MqttPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class MqttPublisherImpl implements MqttPublisher {

    private final MqttClient mqttClient;
    private final ObjectMapper objectMapper;

    // Use @Lazy here to break the circular dependency cycle with MqttConfig -> Services -> MqttPublisher
    public MqttPublisherImpl(@Lazy MqttClient mqttClient, ObjectMapper objectMapper) {
        this.mqttClient = mqttClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishGateCommand(String gateId, String action, String nonce, long timestamp) {
        String topic = "parking/gates/" + gateId + "/control";
        log.info("Publishing gate command to topic: {}, action: {}", topic, action);

        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("action", action);
        payloadMap.put("nonce", nonce);
        payloadMap.put("timestamp", timestamp);

        try {
            String jsonPayload = objectMapper.writeValueAsString(payloadMap);
            MqttMessage message = new MqttMessage(jsonPayload.getBytes());
            message.setQos(2); // Quality of Service 2: Exactly once (critical for gate control)
            mqttClient.publish(topic, message);
            log.info("Successfully published gate command to {}", topic);
        } catch (Exception e) {
            log.error("Failed to publish gate command for gate {}: {}", gateId, e.getMessage());
        }
    }

    @Override
    public void publishSlotCommand(String slotId, String ledColor) {
        String topic = "parking/slots/" + slotId + "/command";
        log.info("Publishing LED color command to topic: {}, color: {}", topic, ledColor);

        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("ledColor", ledColor);

        try {
            String jsonPayload = objectMapper.writeValueAsString(payloadMap);
            MqttMessage message = new MqttMessage(jsonPayload.getBytes());
            message.setQos(1); // At least once
            mqttClient.publish(topic, message);
            log.info("Successfully published LED command to {}", topic);
        } catch (Exception e) {
            log.error("Failed to publish LED command for slot {}: {}", slotId, e.getMessage());
        }
    }
}
