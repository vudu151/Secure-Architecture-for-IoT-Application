package com.smartparking.service.impl;

import com.smartparking.dto.request.BookingRequest;
import com.smartparking.dto.response.BookingDTO;
import com.smartparking.dto.response.SlotDTO;
import com.smartparking.entity.*;
import com.smartparking.enums.BookingStatus;
import com.smartparking.enums.PaymentMethod;
import com.smartparking.enums.PaymentStatus;
import com.smartparking.enums.SlotStatus;
import com.smartparking.exception.BadRequestException;
import com.smartparking.exception.ResourceNotFoundException;
import com.smartparking.exception.SlotNotAvailableException;
import com.smartparking.repository.*;
import com.smartparking.service.AuditService;
import com.smartparking.service.BookingService;
import com.smartparking.service.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final ParkingSlotRepository parkingSlotRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final TransactionRepository transactionRepository;
    private final WalletService walletService;
    private final SimpMessagingTemplate messagingTemplate;
    private final AuditService auditService;

    @Value("${parking.price-per-hour:5000}")
    private double pricePerHour;

    @Override
    @Transactional
    public BookingDTO createBooking(Long userId, BookingRequest request) {
        log.info("Creating booking for user ID: {}, slot ID: {}", userId, request.getSlotId());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + request.getVehicleId()));

        if (!vehicle.getUser().getId().equals(userId)) {
            throw new BadRequestException("This vehicle does not belong to the user");
        }

        // Pessimistic Lock on the parking slot
        ParkingSlot slot = parkingSlotRepository.findByIdForUpdate(request.getSlotId())
                .orElseThrow(() -> new ResourceNotFoundException("Parking slot not found with id: " + request.getSlotId()));

        if (slot.getStatus() != SlotStatus.AVAILABLE) {
            throw new SlotNotAvailableException("Parking slot is already " + slot.getStatus());
        }

        // Reserve the slot
        slot.setStatus(SlotStatus.RESERVED);
        ParkingSlot savedSlot = parkingSlotRepository.save(slot);

        // Generate short booking code and QR data
        String bookingCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String qrCodeData = String.format("{\"bookingCode\":\"%s\",\"slotCode\":\"%s\",\"userId\":%d,\"timestamp\":%d}",
                bookingCode, slot.getSlotCode(), userId, System.currentTimeMillis());

        Booking booking = Booking.builder()
                .user(user)
                .vehicle(vehicle)
                .slot(savedSlot)
                .bookingCode(bookingCode)
                .qrCodeData(qrCodeData)
                .status(BookingStatus.CONFIRMED)
                .bookedFrom(request.getBookedFrom())
                .bookedUntil(request.getBookedUntil())
                .totalAmount(BigDecimal.ZERO)
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        // Broadcast slot status update via WebSocket
        broadcastSlotUpdate(savedSlot);

        auditService.log(userId, "BOOKING_CREATED", "Booking", getClientIp(),
                "Created booking code: " + bookingCode + " for slot: " + slot.getSlotCode());

        return convertToDTO(savedBooking);
    }

    @Override
    @Transactional
    public BookingDTO cancelBooking(Long userId, Long bookingId) {
        log.info("Cancelling booking ID: {} for user ID: {}", bookingId, userId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        if (!booking.getUser().getId().equals(userId)) {
            throw new BadRequestException("You do not own this booking");
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Booking cannot be cancelled in status: " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.CANCELLED);
        ParkingSlot slot = booking.getSlot();
        slot.setStatus(SlotStatus.AVAILABLE);
        
        parkingSlotRepository.save(slot);
        Booking savedBooking = bookingRepository.save(booking);

        // Broadcast slot status update
        broadcastSlotUpdate(slot);

        auditService.log(userId, "BOOKING_CANCELLED", "Booking", getClientIp(),
                "Cancelled booking code: " + booking.getBookingCode());

        return convertToDTO(savedBooking);
    }

    @Override
    public List<BookingDTO> getMyBookings(Long userId) {
        log.info("Fetching bookings for user ID: {}", userId);
        return bookingRepository.findByUserId(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public BookingDTO getBookingById(Long bookingId) {
        log.info("Fetching booking with id: {}", bookingId);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));
        return convertToDTO(booking);
    }

    @Override
    @Transactional
    public BookingDTO checkIn(String bookingCode, String licensePlate) {
        log.info("Processing check-in for booking code: {}, plate: {}", bookingCode, licensePlate);

        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with code: " + bookingCode));

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BadRequestException("Booking cannot be checked in. Current status: " + booking.getStatus());
        }

        // Verify plate (case insensitive, removing special characters if necessary, but here exact match is checked)
        String registeredPlate = booking.getVehicle().getLicensePlate().replaceAll("[^a-zA-Z0-9]", "");
        String incomingPlate = licensePlate.replaceAll("[^a-zA-Z0-9]", "");
        if (!registeredPlate.equalsIgnoreCase(incomingPlate)) {
            auditService.log(booking.getUser().getId(), "CHECK_IN_FAILED", "Booking", getClientIp(),
                    "Check-in failed for booking: " + bookingCode + ". Plate mismatch: Registered " + 
                    booking.getVehicle().getLicensePlate() + ", OCR " + licensePlate);
            throw new BadRequestException("License plate does not match the registered vehicle");
        }

        booking.setCheckedInAt(LocalDateTime.now());
        booking.setStatus(BookingStatus.CHECKED_IN);

        ParkingSlot slot = booking.getSlot();
        slot.setStatus(SlotStatus.OCCUPIED);

        parkingSlotRepository.save(slot);
        Booking savedBooking = bookingRepository.save(booking);

        // Broadcast slot status update
        broadcastSlotUpdate(slot);

        auditService.log(booking.getUser().getId(), "VEHICLE_CHECK_IN", "Booking", getClientIp(),
                "Checked in vehicle: " + licensePlate + " at slot: " + slot.getSlotCode());

        return convertToDTO(savedBooking);
    }

    @Override
    @Transactional
    public BookingDTO checkOut(String bookingCode) {
        log.info("Processing check-out for booking code: {}", bookingCode);

        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with code: " + bookingCode));

        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw new BadRequestException("Booking is not in CHECKED_IN state. Current status: " + booking.getStatus());
        }

        LocalDateTime checkedOutAt = LocalDateTime.now();
        booking.setCheckedOutAt(checkedOutAt);

        // Calculate hours parked (minimum 1 hour)
        Duration duration = Duration.between(booking.getCheckedInAt(), checkedOutAt);
        long hours = duration.toHours();
        if (duration.toMinutes() % 60 > 0 || hours == 0) {
            hours += 1;
        }

        BigDecimal totalAmount = BigDecimal.valueOf(hours).multiply(BigDecimal.valueOf(pricePerHour));
        booking.setTotalAmount(totalAmount);
        booking.setStatus(BookingStatus.COMPLETED);

        // Deduct wallet balance
        walletService.deduct(booking.getUser().getId(), totalAmount);

        // Save transaction
        Transaction tx = Transaction.builder()
                .booking(booking)
                .user(booking.getUser())
                .amount(totalAmount)
                .paymentMethod(PaymentMethod.WALLET)
                .paymentStatus(PaymentStatus.COMPLETED)
                .transactionRef("CHECKOUT_" + bookingCode)
                .build();
        transactionRepository.save(tx);

        ParkingSlot slot = booking.getSlot();
        slot.setStatus(SlotStatus.AVAILABLE);

        parkingSlotRepository.save(slot);
        Booking savedBooking = bookingRepository.save(booking);

        // Broadcast slot status update
        broadcastSlotUpdate(slot);

        auditService.log(booking.getUser().getId(), "VEHICLE_CHECK_OUT", "Booking", getClientIp(),
                "Checked out vehicle: " + booking.getVehicle().getLicensePlate() + 
                " from slot: " + slot.getSlotCode() + ". Charged: " + totalAmount + " VND");

        return convertToDTO(savedBooking);
    }

    @Override
    @Scheduled(fixedRate = 60000) // Every 1 minute
    @Transactional
    public void autoExpireBookings() {
        log.debug("Running auto expire scheduler for bookings");
        
        // Find bookings CONFIRMED but not checked in where booked_from is over 20 minutes ago
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(20);
        
        // We'll fetch all CONFIRMED bookings and check manually since the method in Repository findByStatusAndBookedUntilBefore is for bookedUntil.
        // Actually, we want to expire CONFIRMED bookings that passed their bookedFrom time by 20 minutes.
        List<Booking> confirmedBookings = bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED && b.getBookedFrom().isBefore(cutoff))
                .collect(Collectors.toList());

        for (Booking booking : confirmedBookings) {
            log.info("Expiring booking: {}", booking.getBookingCode());
            booking.setStatus(BookingStatus.EXPIRED);
            
            ParkingSlot slot = booking.getSlot();
            slot.setStatus(SlotStatus.AVAILABLE);
            
            parkingSlotRepository.save(slot);
            bookingRepository.save(booking);

            broadcastSlotUpdate(slot);

            auditService.log(booking.getUser().getId(), "BOOKING_EXPIRED", "Booking", "0.0.0.0",
                    "Expired booking code: " + booking.getBookingCode() + " due to check-in timeout");
        }
    }

    private void broadcastSlotUpdate(ParkingSlot slot) {
        try {
            SlotDTO dto = SlotDTO.builder()
                    .id(slot.getId())
                    .slotCode(slot.getSlotCode())
                    .zone(slot.getZone())
                    .status(slot.getStatus())
                    .sensorId(slot.getSensorId())
                    .build();
            messagingTemplate.convertAndSend("/topic/slots", dto);
        } catch (Exception e) {
            log.error("Failed to broadcast slot update via WebSocket: {}", e.getMessage());
        }
    }

    private BookingDTO convertToDTO(Booking b) {
        return BookingDTO.builder()
                .id(b.getId())
                .slotCode(b.getSlot().getSlotCode())
                .zone(b.getSlot().getZone())
                .vehiclePlate(b.getVehicle().getLicensePlate())
                .bookingCode(b.getBookingCode())
                .qrCodeData(b.getQrCodeData())
                .status(b.getStatus())
                .bookedFrom(b.getBookedFrom())
                .bookedUntil(b.getBookedUntil())
                .checkedInAt(b.getCheckedInAt())
                .checkedOutAt(b.getCheckedOutAt())
                .totalAmount(b.getTotalAmount())
                .createdAt(b.getCreatedAt())
                .build();
    }

    private String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            return ip;
        }
        return "0.0.0.0";
    }
}
