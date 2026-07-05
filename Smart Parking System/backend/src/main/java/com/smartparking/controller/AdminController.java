package com.smartparking.controller;

import com.smartparking.dto.request.GateControlRequest;
import com.smartparking.dto.response.*;
import com.smartparking.service.AdminService;
import com.smartparking.service.AuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final AuditService auditService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardDTO>> getDashboard() {
        log.info("Admin request for dashboard metrics");
        DashboardDTO dto = adminService.getDashboard();
        return ResponseEntity.ok(ApiResponse.success("Dashboard metrics retrieved successfully", dto));
    }

    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<List<RevenueReportDTO>>> getRevenueReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        log.info("Admin request for revenue report from {} to {}", from, to);
        List<RevenueReportDTO> report = adminService.getRevenueReport(from, to);
        return ResponseEntity.ok(ApiResponse.success("Revenue report retrieved successfully", report));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserDTO>>> getAllUsers() {
        log.info("Admin request for all users");
        List<UserDTO> users = adminService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success("All users retrieved successfully", users));
    }

    @PutMapping("/users/{id}/toggle-active")
    public ResponseEntity<ApiResponse<UserDTO>> toggleUserActive(@PathVariable Long id) {
        log.info("Admin request to toggle active status for user ID: {}", id);
        UserDTO user = adminService.toggleUserActive(id);
        return ResponseEntity.ok(ApiResponse.success("User active status toggled successfully", user));
    }

    @GetMapping("/devices")
    public ResponseEntity<ApiResponse<List<DeviceDTO>>> getAllDevices() {
        log.info("Admin request for all devices");
        List<DeviceDTO> devices = adminService.getAllDevices();
        return ResponseEntity.ok(ApiResponse.success("All devices retrieved successfully", devices));
    }

    @PostMapping("/gate/{id}/control")
    public ResponseEntity<ApiResponse<Void>> controlGate(
            @PathVariable String id,
            @Valid @RequestBody GateControlRequest request
    ) {
        log.info("Admin request to control gate: {} with action: {}", id, request.getAction());
        adminService.controlGate(id, request.getAction());
        return ResponseEntity.ok(ApiResponse.success("Gate command sent successfully", null));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<Page<AuditLogDTO>>> getAuditLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            Pageable pageable
    ) {
        log.info("Admin request for audit logs");
        Page<AuditLogDTO> logs = auditService.getLogs(userId, action, from, to, pageable);
        return ResponseEntity.ok(ApiResponse.success("Audit logs retrieved successfully", logs));
    }
}
