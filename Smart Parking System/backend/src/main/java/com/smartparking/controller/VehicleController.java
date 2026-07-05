package com.smartparking.controller;

import com.smartparking.dto.request.VehicleRequest;
import com.smartparking.dto.response.ApiResponse;
import com.smartparking.dto.response.VehicleDTO;
import com.smartparking.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
@Slf4j
public class VehicleController {

    private final VehicleService vehicleService;

    @GetMapping("/my")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<List<VehicleDTO>>> getMyVehicles() {
        Long userId = getUserId();
        log.info("Fetching vehicles for user id: {}", userId);
        List<VehicleDTO> vehicles = vehicleService.getMyVehicles(userId);
        return ResponseEntity.ok(ApiResponse.success("User vehicles retrieved successfully", vehicles));
    }

    @PostMapping
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<VehicleDTO>> addVehicle(@Valid @RequestBody VehicleRequest request) {
        Long userId = getUserId();
        log.info("Adding new vehicle for user id: {}, plate: {}", userId, request.getLicensePlate());
        VehicleDTO vehicle = vehicleService.addVehicle(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Vehicle registered successfully", vehicle));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<Void>> deleteVehicle(@PathVariable Long id) {
        Long userId = getUserId();
        log.info("Deleting vehicle with id: {} for user id: {}", id, userId);
        vehicleService.deleteVehicle(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Vehicle deleted successfully", null));
    }

    private Long getUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getCredentials();
    }
}
