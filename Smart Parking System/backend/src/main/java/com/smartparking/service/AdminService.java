package com.smartparking.service;

import com.smartparking.dto.response.*;

import java.time.LocalDate;
import java.util.List;

public interface AdminService {
    DashboardDTO getDashboard();
    List<RevenueReportDTO> getRevenueReport(LocalDate from, LocalDate to);
    List<UserDTO> getAllUsers();
    UserDTO toggleUserActive(Long userId);
    List<DeviceDTO> getAllDevices();
    void controlGate(String gateId, String action);
}
