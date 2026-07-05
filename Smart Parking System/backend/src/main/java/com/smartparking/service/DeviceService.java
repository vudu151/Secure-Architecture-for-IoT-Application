package com.smartparking.service;

import com.smartparking.dto.response.BookingDTO;

public interface DeviceService {
    BookingDTO verifyPlate(String licensePlate, String gateId);
    BookingDTO verifyQr(String qrData, String gateId);
    void registerHeartbeat(String deviceUid, String firmwareVersion);
    void verifyNonce(String nonce, long timestamp);
}
