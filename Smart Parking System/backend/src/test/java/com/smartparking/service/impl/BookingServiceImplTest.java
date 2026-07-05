package com.smartparking.service.impl;

import com.smartparking.dto.request.BookingRequest;
import com.smartparking.dto.response.BookingDTO;
import com.smartparking.dto.response.SlotDTO;
import com.smartparking.entity.*;
import com.smartparking.enums.BookingStatus;
import com.smartparking.enums.PaymentMethod;
import com.smartparking.enums.PaymentStatus;
import com.smartparking.enums.SlotStatus;
import com.smartparking.enums.UserRole;
import com.smartparking.exception.BadRequestException;
import com.smartparking.exception.ResourceNotFoundException;
import com.smartparking.exception.SlotNotAvailableException;
import com.smartparking.repository.*;
import com.smartparking.service.AuditService;
import com.smartparking.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private ParkingSlotRepository parkingSlotRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private WalletService walletService;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private User testUser;
    private Vehicle testVehicle;
    private ParkingSlot testSlot;
    private Booking testBooking;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(bookingService, "pricePerHour", 5000.0);

        testUser = User.builder()
                .id(1L)
                .email("driver@smartparking.com")
                .fullName("Driver User")
                .role(UserRole.DRIVER)
                .balance(new BigDecimal("20000.00"))
                .build();

        testVehicle = Vehicle.builder()
                .id(1L)
                .user(testUser)
                .licensePlate("30A12345")
                .vehicleType("CAR")
                .build();

        testSlot = ParkingSlot.builder()
                .id(1L)
                .slotCode("A01")
                .zone("Zone A")
                .status(SlotStatus.AVAILABLE)
                .build();

        testBooking = Booking.builder()
                .id(1L)
                .user(testUser)
                .vehicle(testVehicle)
                .slot(testSlot)
                .bookingCode("BK123456")
                .qrCodeData("{}")
                .status(BookingStatus.CONFIRMED)
                .bookedFrom(LocalDateTime.now().minusHours(1))
                .bookedUntil(LocalDateTime.now().plusHours(1))
                .totalAmount(BigDecimal.ZERO)
                .build();
    }

    @Test
    void testCreateBookingSuccess() {
        BookingRequest request = new BookingRequest();
        request.setSlotId(1L);
        request.setVehicleId(1L);
        request.setBookedFrom(LocalDateTime.now());
        request.setBookedUntil(LocalDateTime.now().plusHours(2));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(testVehicle));
        when(parkingSlotRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testSlot));
        when(parkingSlotRepository.save(any(ParkingSlot.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(99L);
            return b;
        });

        BookingDTO dto = bookingService.createBooking(1L, request);

        assertNotNull(dto);
        assertEquals(99L, dto.getId());
        assertEquals("A01", dto.getSlotCode());
        assertEquals(BookingStatus.CONFIRMED, dto.getStatus());
        assertEquals(SlotStatus.RESERVED, testSlot.getStatus());
        
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/slots"), any(SlotDTO.class));
        verify(auditService, times(1)).log(eq(1L), eq("BOOKING_CREATED"), eq("Booking"), eq("0.0.0.0"), anyString());
    }

    @Test
    void testCreateBookingSlotNotAvailable() {
        BookingRequest request = new BookingRequest();
        request.setSlotId(1L);
        request.setVehicleId(1L);

        testSlot.setStatus(SlotStatus.OCCUPIED); // Not available

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(testVehicle));
        when(parkingSlotRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testSlot));

        assertThrows(SlotNotAvailableException.class, () -> bookingService.createBooking(1L, request));
    }

    @Test
    void testCancelBookingSuccess() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(parkingSlotRepository.save(any(ParkingSlot.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingDTO dto = bookingService.cancelBooking(1L, 1L);

        assertNotNull(dto);
        assertEquals(BookingStatus.CANCELLED, dto.getStatus());
        assertEquals(SlotStatus.AVAILABLE, testSlot.getStatus());
        
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/slots"), any(SlotDTO.class));
    }

    @Test
    void testCancelBookingInvalidState() {
        testBooking.setStatus(BookingStatus.COMPLETED); // Already finished, can't cancel

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));

        assertThrows(BadRequestException.class, () -> bookingService.cancelBooking(1L, 1L));
    }

    @Test
    void testCheckInSuccess() {
        when(bookingRepository.findByBookingCode("BK123456")).thenReturn(Optional.of(testBooking));
        when(parkingSlotRepository.save(any(ParkingSlot.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        // Incoming plate matches registered vehicle: "30A-123.45" (ignore spaces & symbols)
        BookingDTO dto = bookingService.checkIn("BK123456", "30A-123.45");

        assertNotNull(dto);
        assertEquals(BookingStatus.CHECKED_IN, dto.getStatus());
        assertEquals(SlotStatus.OCCUPIED, testSlot.getStatus());
        assertNotNull(testBooking.getCheckedInAt());
    }

    @Test
    void testCheckInPlateMismatch() {
        when(bookingRepository.findByBookingCode("BK123456")).thenReturn(Optional.of(testBooking));

        // Different plate: "29B99999"
        assertThrows(BadRequestException.class, () -> bookingService.checkIn("BK123456", "29B99999"));
        verify(auditService, times(1)).log(eq(1L), eq("CHECK_IN_FAILED"), eq("Booking"), eq("0.0.0.0"), anyString());
    }

    @Test
    void testCheckOutSuccess() {
        testBooking.setStatus(BookingStatus.CHECKED_IN);
        // Checked in 1.5 hours ago
        testBooking.setCheckedInAt(LocalDateTime.now().minusMinutes(90)); 

        when(bookingRepository.findByBookingCode("BK123456")).thenReturn(Optional.of(testBooking));
        when(parkingSlotRepository.save(any(ParkingSlot.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingDTO dto = bookingService.checkOut("BK123456");

        assertNotNull(dto);
        assertEquals(BookingStatus.COMPLETED, dto.getStatus());
        assertEquals(SlotStatus.AVAILABLE, testSlot.getStatus());
        // 90 minutes parked -> counts as 2 hours -> 2 * 5000 = 10000 VND
        assertEquals(new BigDecimal("10000.0"), dto.getTotalAmount());
        
        // Verify deduction called
        verify(walletService, times(1)).deduct(eq(1L), eq(new BigDecimal("10000.0")));
        
        // Verify transaction logged
        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(1)).save(txCaptor.capture());
        Transaction tx = txCaptor.getValue();
        assertEquals(new BigDecimal("10000.0"), tx.getAmount());
        assertEquals(PaymentMethod.WALLET, tx.getPaymentMethod());
        assertEquals(PaymentStatus.COMPLETED, tx.getPaymentStatus());
    }
}
