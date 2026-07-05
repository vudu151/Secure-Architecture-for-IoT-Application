package com.smartparking.service.impl;

import com.smartparking.dto.response.BookingDTO;
import com.smartparking.entity.Booking;
import com.smartparking.entity.DeviceRegistry;
import com.smartparking.entity.Vehicle;
import com.smartparking.enums.BookingStatus;
import com.smartparking.enums.DeviceType;
import com.smartparking.exception.BadRequestException;
import com.smartparking.exception.ResourceNotFoundException;
import com.smartparking.repository.BookingRepository;
import com.smartparking.repository.DeviceRegistryRepository;
import com.smartparking.repository.VehicleRepository;
import com.smartparking.service.BookingService;
import com.smartparking.service.DeviceService;
import com.smartparking.service.MqttPublisher;
import com.smartparking.service.NonceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRegistryRepository deviceRegistryRepository;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final VehicleRepository vehicleRepository;
    private final MqttPublisher mqttPublisher;
    private final NonceService nonceService;

    @Override
    @Transactional
    public BookingDTO verifyPlate(String licensePlate, String gateId) {
        log.info("Verifying vehicle plate: {} at gate: {}", licensePlate, gateId);
        
        String cleanPlate = licensePlate.replaceAll("[^a-zA-Z0-9]", "");
        
        Vehicle vehicle = vehicleRepository.findAll().stream()
                .filter(v -> v.getLicensePlate().replaceAll("[^a-zA-Z0-9]", "").equalsIgnoreCase(cleanPlate))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not registered in system: " + licensePlate));

        // Find active booking for this vehicle
        List<Booking> bookings = bookingRepository.findAll().stream()
                .filter(b -> b.getVehicle().getId().equals(vehicle.getId()))
                .collect(Collectors.toList());

        // 1. Try to find a CONFIRMED booking to check in
        Booking checkInBooking = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .findFirst()
                .orElse(null);

        if (checkInBooking != null) {
            BookingDTO dto = bookingService.checkIn(checkInBooking.getBookingCode(), vehicle.getLicensePlate());
            triggerGateOpen(gateId);
            return dto;
        }

        // 2. Try to find a CHECKED_IN booking to check out
        Booking checkOutBooking = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CHECKED_IN)
                .findFirst()
                .orElse(null);

        if (checkOutBooking != null) {
            BookingDTO dto = bookingService.checkOut(checkOutBooking.getBookingCode());
            triggerGateOpen(gateId);
            return dto;
        }

        throw new BadRequestException("No active booking found for vehicle: " + licensePlate);
    }

    @Override
    @Transactional
    public BookingDTO verifyQr(String qrData, String gateId) {
        log.info("Verifying QR data at gate: {}", gateId);
        
        String bookingCode = parseBookingCode(qrData);
        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with code: " + bookingCode));

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            BookingDTO dto = bookingService.checkIn(bookingCode, booking.getVehicle().getLicensePlate());
            triggerGateOpen(gateId);
            return dto;
        } else if (booking.getStatus() == BookingStatus.CHECKED_IN) {
            BookingDTO dto = bookingService.checkOut(bookingCode);
            triggerGateOpen(gateId);
            return dto;
        }

        throw new BadRequestException("Booking code " + bookingCode + " is in invalid status: " + booking.getStatus());
    }

    @Override
    @Transactional
    public void registerHeartbeat(String deviceUid, String firmwareVersion) {
        log.debug("Received heartbeat from device UID: {}", deviceUid);
        
        DeviceRegistry device = deviceRegistryRepository.findByDeviceUid(deviceUid)
                .orElseGet(() -> {
                    log.info("Registering new device UID: {}", deviceUid);
                    DeviceType type = deviceUid.toLowerCase().contains("gate") ? DeviceType.GATE : DeviceType.SENSOR;
                    return DeviceRegistry.builder()
                            .deviceUid(deviceUid)
                            .deviceType(type)
                            .location("Unknown")
                            .certificateCn(deviceUid)
                            .build();
                });

        device.setIsOnline(true);
        device.setLastHeartbeat(LocalDateTime.now());
        if (firmwareVersion != null) {
            device.setFirmwareVersion(firmwareVersion);
        }
        
        deviceRegistryRepository.save(device);
    }

    private String parseBookingCode(String qrData) {
        if (qrData.contains("bookingCode")) {
            int idx = qrData.indexOf("\"bookingCode\":\"");
            if (idx != -1) {
                int start = idx + 15;
                int end = qrData.indexOf("\"", start);
                if (end != -1) {
                    return qrData.substring(start, end);
                }
            }
        }
        return qrData.trim(); // Fallback if raw booking code is scanned
    }

    private void triggerGateOpen(String gateId) {
        try {
            String nonce = nonceService.generateNonce();
            long timestamp = System.currentTimeMillis();
            mqttPublisher.publishGateCommand(gateId, "OPEN", nonce, timestamp);
            log.info("Triggered gate open via MQTT for gate: {}", gateId);
        } catch (Exception e) {
            log.error("Failed to trigger gate open via MQTT: {}", e.getMessage());
        }
    }

    @Override
    public void verifyNonce(String nonce, long timestamp) {
        nonceService.validateNonce(nonce, timestamp);
    }
}
