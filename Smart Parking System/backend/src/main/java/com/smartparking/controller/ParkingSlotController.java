package com.smartparking.controller;

import com.smartparking.dto.response.ApiResponse;
import com.smartparking.dto.response.SlotDTO;
import com.smartparking.service.ParkingSlotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/slots")
@RequiredArgsConstructor
@Slf4j
public class ParkingSlotController {

    private final ParkingSlotService parkingSlotService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SlotDTO>>> getAllSlots() {
        log.info("Fetching all parking slots");
        List<SlotDTO> slots = parkingSlotService.getAllSlots();
        return ResponseEntity.ok(ApiResponse.success("All parking slots retrieved successfully", slots));
    }

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<SlotDTO>>> getAvailableSlots() {
        log.info("Fetching available parking slots");
        List<SlotDTO> slots = parkingSlotService.getAvailableSlots();
        return ResponseEntity.ok(ApiResponse.success("Available parking slots retrieved successfully", slots));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SlotDTO>> getSlotById(@PathVariable Long id) {
        log.info("Fetching parking slot with id: {}", id);
        SlotDTO slot = parkingSlotService.getSlotById(id);
        return ResponseEntity.ok(ApiResponse.success("Parking slot retrieved successfully", slot));
    }
}
