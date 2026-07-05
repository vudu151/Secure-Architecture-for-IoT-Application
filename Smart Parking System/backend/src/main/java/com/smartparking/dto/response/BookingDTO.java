package com.smartparking.dto.response;

import com.smartparking.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingDTO {
    private Long id;
    private String slotCode;
    private String zone;
    private String vehiclePlate;
    private String bookingCode;
    private String qrCodeData;
    private BookingStatus status;
    private LocalDateTime bookedFrom;
    private LocalDateTime bookedUntil;
    private LocalDateTime checkedInAt;
    private LocalDateTime checkedOutAt;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
}
