package com.smartparking.repository;

import com.smartparking.entity.Booking;
import com.smartparking.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserId(Long userId);
    Optional<Booking> findByBookingCode(String bookingCode);
    List<Booking> findBySlotIdAndStatusIn(Long slotId, Collection<BookingStatus> statuses);
    List<Booking> findByStatusAndBookedUntilBefore(BookingStatus status, LocalDateTime dateTime);
}
