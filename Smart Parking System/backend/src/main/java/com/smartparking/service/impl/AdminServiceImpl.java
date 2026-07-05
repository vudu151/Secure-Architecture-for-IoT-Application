package com.smartparking.service.impl;

import com.smartparking.dto.response.*;
import com.smartparking.entity.*;
import com.smartparking.enums.DeviceType;
import com.smartparking.enums.PaymentStatus;
import com.smartparking.enums.SlotStatus;
import com.smartparking.enums.UserRole;
import com.smartparking.exception.ResourceNotFoundException;
import com.smartparking.repository.*;
import com.smartparking.service.AdminService;
import com.smartparking.service.AuditService;
import com.smartparking.service.MqttPublisher;
import com.smartparking.service.NonceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final ParkingSlotRepository parkingSlotRepository;
    private final BookingRepository bookingRepository;
    private final TransactionRepository transactionRepository;
    private final DeviceRegistryRepository deviceRegistryRepository;
    private final NonceService nonceService;
    private final MqttPublisher mqttPublisher;
    private final AuditService auditService;

    @Override
    public DashboardDTO getDashboard() {
        log.info("Generating dashboard statistics");
        
        List<ParkingSlot> slots = parkingSlotRepository.findAll();
        int totalSlots = slots.size();
        int occupiedSlots = (int) slots.stream().filter(s -> s.getStatus() == SlotStatus.OCCUPIED).count();
        int availableSlots = (int) slots.stream().filter(s -> s.getStatus() == SlotStatus.AVAILABLE).count();
        int reservedSlots = (int) slots.stream().filter(s -> s.getStatus() == SlotStatus.RESERVED).count();

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        
        BigDecimal revenueToday = transactionRepository.findAll().stream()
                .filter(t -> t.getCreatedAt().isAfter(startOfDay) && t.getPaymentStatus() == PaymentStatus.COMPLETED)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalBookingsToday = (int) bookingRepository.findAll().stream()
                .filter(b -> b.getCreatedAt().isAfter(startOfDay))
                .count();

        return DashboardDTO.builder()
                .totalSlots(totalSlots)
                .occupiedSlots(occupiedSlots)
                .availableSlots(availableSlots)
                .reservedSlots(reservedSlots)
                .revenueToday(revenueToday)
                .totalBookingsToday(totalBookingsToday)
                .build();
    }

    @Override
    public List<RevenueReportDTO> getRevenueReport(LocalDate from, LocalDate to) {
        log.info("Generating revenue report from {} to {}", from, to);
        
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();

        List<Transaction> transactions = transactionRepository.findAll().stream()
                .filter(t -> t.getCreatedAt().isAfter(start) && t.getCreatedAt().isBefore(end) && t.getPaymentStatus() == PaymentStatus.COMPLETED)
                .collect(Collectors.toList());

        Map<LocalDate, List<Transaction>> grouped = transactions.stream()
                .collect(Collectors.groupingBy(t -> t.getCreatedAt().toLocalDate()));

        return grouped.entrySet().stream()
                .map(entry -> {
                    BigDecimal dailyRevenue = entry.getValue().stream()
                            .map(Transaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    long bookingsCount = entry.getValue().stream()
                            .filter(t -> t.getBooking() != null)
                            .map(t -> t.getBooking().getId())
                            .distinct()
                            .count();
                    
                    return RevenueReportDTO.builder()
                            .date(entry.getKey())
                            .revenue(dailyRevenue)
                            .bookingsCount((int) bookingsCount)
                            .build();
                })
                .sorted((r1, r2) -> r1.getDate().compareTo(r2.getDate()))
                .collect(Collectors.toList());
    }

    @Override
    public List<UserDTO> getAllUsers() {
        log.info("Fetching all users");
        return userRepository.findAll().stream()
                .map(this::convertToUserDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserDTO toggleUserActive(Long userId) {
        log.info("Toggling active status for user ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setIsActive(!user.getIsActive());
        User savedUser = userRepository.save(user);

        auditService.log(null, "USER_STATUS_TOGGLE", "User", "0.0.0.0", 
                "Toggled active status for user ID: " + userId + " to: " + savedUser.getIsActive());

        return convertToUserDTO(savedUser);
    }

    @Override
    public List<DeviceDTO> getAllDevices() {
        log.info("Fetching all registered devices");
        return deviceRegistryRepository.findAll().stream()
                .map(this::convertToDeviceDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void controlGate(String gateId, String action) {
        log.info("Sending control command to gate: {}, action: {}", gateId, action);
        
        String nonce = nonceService.generateNonce();
        long timestamp = System.currentTimeMillis();

        mqttPublisher.publishGateCommand(gateId, action, nonce, timestamp);

        auditService.log(null, "GATE_CONTROL", "Gate", "0.0.0.0", 
                "Sent gate control command: " + action + " to gate ID: " + gateId);
    }

    private UserDTO convertToUserDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole())
                .balance(user.getBalance())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private DeviceDTO convertToDeviceDTO(DeviceRegistry device) {
        return DeviceDTO.builder()
                .id(device.getId())
                .deviceUid(device.getDeviceUid())
                .deviceType(device.getDeviceType())
                .location(device.getLocation())
                .isOnline(device.getIsOnline())
                .lastHeartbeat(device.getLastHeartbeat())
                .firmwareVersion(device.getFirmwareVersion())
                .build();
    }
}
