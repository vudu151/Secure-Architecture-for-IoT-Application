package com.smartparking.service;

public interface MqttPublisher {
    void publishGateCommand(String gateId, String action, String nonce, long timestamp);
    void publishSlotCommand(String slotId, String ledColor);
}
