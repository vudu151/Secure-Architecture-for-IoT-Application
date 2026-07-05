package com.smartparking.controller;

import com.smartparking.dto.request.BookingRequest;
import com.smartparking.dto.response.ApiResponse;
import com.smartparking.dto.response.BookingDTO;
import com.smartparking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Slf4j
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<BookingDTO>> createBooking(@Valid @RequestBody BookingRequest request) {
        Long userId = getUserId();
        log.info("Received booking creation request from user ID {} for slot ID {}", userId, request.getSlotId());
        BookingDTO booking = bookingService.createBooking(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Booking created successfully", booking));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<List<BookingDTO>>> getMyBookings() {
        Long userId = getUserId();
        log.info("Received request for bookings of user ID {}", userId);
        List<BookingDTO> bookings = bookingService.getMyBookings(userId);
        return ResponseEntity.ok(ApiResponse.success("User bookings retrieved successfully", bookings));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingDTO>> getBookingById(@PathVariable Long id) {
        log.info("Received request to fetch booking ID {}", id);
        BookingDTO booking = bookingService.getBookingById(id);
        return ResponseEntity.ok(ApiResponse.success("Booking retrieved successfully", booking));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<BookingDTO>> cancelBooking(@PathVariable Long id) {
        Long userId = getUserId();
        log.info("Received request to cancel booking ID {} from user ID {}", id, userId);
        BookingDTO booking = bookingService.cancelBooking(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled successfully", booking));
    }

    @PostMapping("/{id}/check-in")
    public ResponseEntity<ApiResponse<BookingDTO>> checkIn(@PathVariable Long id, @RequestParam String licensePlate) {
        log.info("Received check-in request for booking ID {} with plate {}", id, licensePlate);
        BookingDTO booking = bookingService.getBookingById(id);
        BookingDTO result = bookingService.checkIn(booking.getBookingCode(), licensePlate);
        return ResponseEntity.ok(ApiResponse.success("Checked in successfully", result));
    }

    @PostMapping("/{id}/check-out")
    public ResponseEntity<ApiResponse<BookingDTO>> checkOut(@PathVariable Long id) {
        log.info("Received check-out request for booking ID {}", id);
        BookingDTO booking = bookingService.getBookingById(id);
        BookingDTO result = bookingService.checkOut(booking.getBookingCode());
        return ResponseEntity.ok(ApiResponse.success("Checked out successfully", result));
    }

    private Long getUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getCredentials();
    }
}
