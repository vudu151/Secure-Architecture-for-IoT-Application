package com.smartparking.controller;

import com.smartparking.dto.request.SlotStatusUpdateRequest;
import com.smartparking.dto.response.ApiResponse;
import com.smartparking.dto.response.BookingDTO;
import com.smartparking.dto.response.SlotDTO;
import com.smartparking.service.DeviceService;
import com.smartparking.service.ParkingSlotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
@Slf4j
public class DeviceController {

    private final DeviceService deviceService;
    private final ParkingSlotService parkingSlotService;

    @PostMapping("/slot-status")
    public ResponseEntity<ApiResponse<SlotDTO>> updateSlotStatus(
            @RequestHeader("X-Slot-Code") String slotCode,
            @Valid @RequestBody SlotStatusUpdateRequest request
    ) {
        log.info("Received slot status update from device for slot: {}, occupied: {}", slotCode, request.getOccupied());
        SlotDTO dto = parkingSlotService.updateSlotStatus(slotCode, request.getOccupied());
        return ResponseEntity.ok(ApiResponse.success("Slot status updated successfully", dto));
    }

    @PostMapping("/verify-plate")
    public ResponseEntity<ApiResponse<BookingDTO>> verifyPlate(
            @RequestParam String plate,
            @RequestParam String gateId
    ) {
        log.info("Received vehicle plate verification request for plate: {} at gate: {}", plate, gateId);
        BookingDTO dto = deviceService.verifyPlate(plate, gateId);
        return ResponseEntity.ok(ApiResponse.success("License plate verified successfully", dto));
    }

    @PostMapping("/verify-qr")
    public ResponseEntity<ApiResponse<BookingDTO>> verifyQr(
            @RequestParam String qrData,
            @RequestParam String gateId
    ) {
        log.info("Received QR verification request at gate: {}", gateId);
        BookingDTO dto = deviceService.verifyQr(qrData, gateId);
        return ResponseEntity.ok(ApiResponse.success("QR code verified successfully", dto));
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<ApiResponse<Void>> heartbeat(
            @RequestParam String deviceUid,
            @RequestParam(required = false) String firmwareVersion
    ) {
        log.debug("Received heartbeat request from device: {}", deviceUid);
        deviceService.registerHeartbeat(deviceUid, firmwareVersion);
        return ResponseEntity.ok(ApiResponse.success("Heartbeat registered successfully", null));
    }

    @PostMapping("/verify-nonce")
    public ResponseEntity<ApiResponse<Void>> verifyNonce(
            @RequestParam String nonce,
            @RequestParam long timestamp
    ) {
        log.info("Received nonce verification request. Nonce: {}, Timestamp: {}", nonce, timestamp);
        deviceService.verifyNonce(nonce, timestamp);
        return ResponseEntity.ok(ApiResponse.success("Nonce validated successfully", null));
    }
}
