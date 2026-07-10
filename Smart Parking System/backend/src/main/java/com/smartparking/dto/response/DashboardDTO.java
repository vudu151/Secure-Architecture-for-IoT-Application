package com.smartparking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDTO {
    private int totalSlots;
    private int occupiedSlots;
    private int availableSlots;
    private int reservedSlots;
    private BigDecimal revenueToday;
    private int totalBookingsToday;
    private java.util.List<BookingDTO> recentBookings;
    private java.util.List<HourlyTrafficDTO> hourlyTraffic;
}
