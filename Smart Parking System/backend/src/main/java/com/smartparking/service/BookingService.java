package com.smartparking.service;

import com.smartparking.dto.request.BookingRequest;
import com.smartparking.dto.response.BookingDTO;

import java.util.List;

public interface BookingService {
    BookingDTO createBooking(Long userId, BookingRequest request);
    BookingDTO cancelBooking(Long userId, Long bookingId);
    List<BookingDTO> getMyBookings(Long userId);
    BookingDTO getBookingById(Long bookingId);
    BookingDTO checkIn(String bookingCode, String licensePlate);
    BookingDTO checkOut(String bookingCode);
    void autoExpireBookings();
}
