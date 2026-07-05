package com.smartparking.service.impl;

import com.smartparking.entity.User;
import com.smartparking.entity.Transaction;
import com.smartparking.enums.PaymentMethod;
import com.smartparking.enums.PaymentStatus;
import com.smartparking.enums.UserRole;
import com.smartparking.exception.InsufficientBalanceException;
import com.smartparking.exception.ResourceNotFoundException;
import com.smartparking.repository.TransactionRepository;
import com.smartparking.repository.UserRepository;
import com.smartparking.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private WalletServiceImpl walletService;

    private User testUser;
    private final Long userId = 1L;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(userId)
                .email("driver@smartparking.com")
                .fullName("Driver User")
                .role(UserRole.DRIVER)
                .balance(new BigDecimal("10000.00")) // 10,000 VND
                .isActive(true)
                .build();
    }

    @Test
    void testGetBalanceSuccess() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        BigDecimal balance = walletService.getBalance(userId);
        assertEquals(new BigDecimal("10000.00"), balance);
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void testGetBalanceUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> walletService.getBalance(userId));
    }

    @Test
    void testTopupSuccess() {
        BigDecimal topupAmount = new BigDecimal("50000.00");
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        
        // Mock save user returning updated balance
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BigDecimal newBalance = walletService.topup(userId, topupAmount);
        
        assertEquals(new BigDecimal("60000.00"), newBalance);
        assertEquals(new BigDecimal("60000.00"), testUser.getBalance());
        
        // Verify transaction saved
        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(1)).save(txCaptor.capture());
        Transaction savedTx = txCaptor.getValue();
        assertEquals(topupAmount, savedTx.getAmount());
        assertEquals(PaymentMethod.BANK_TRANSFER, savedTx.getPaymentMethod());
        assertEquals(PaymentStatus.COMPLETED, savedTx.getPaymentStatus());
        assertNotNull(savedTx.getTransactionRef());
        
        // Verify audit logged
        verify(auditService, times(1)).log(
                eq(userId), eq("WALLET_TOPUP"), eq("Wallet"), eq("0.0.0.0"), anyString()
        );
    }

    @Test
    void testTopupNegativeOrZeroAmount() {
        assertThrows(IllegalArgumentException.class, () -> walletService.topup(userId, new BigDecimal("-10.00")));
        assertThrows(IllegalArgumentException.class, () -> walletService.topup(userId, BigDecimal.ZERO));
        
        verifyNoInteractions(userRepository);
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void testDeductSuccess() {
        BigDecimal deductAmount = new BigDecimal("5000.00");
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        walletService.deduct(userId, deductAmount);

        assertEquals(new BigDecimal("5000.00"), testUser.getBalance());
        verify(userRepository, times(1)).save(testUser);
        verify(auditService, times(1)).log(
                eq(userId), eq("WALLET_DEDUCT"), eq("Wallet"), eq("0.0.0.0"), anyString()
        );
    }

    @Test
    void testDeductInsufficientBalance() {
        BigDecimal deductAmount = new BigDecimal("15000.00"); // balance is 10000
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        assertThrows(InsufficientBalanceException.class, () -> walletService.deduct(userId, deductAmount));

        // Balance should not change
        assertEquals(new BigDecimal("10000.00"), testUser.getBalance());
        verify(userRepository, never()).save(any(User.class));
        verify(auditService, times(1)).log(
                eq(userId), eq("WALLET_DEDUCT_FAILED"), eq("Wallet"), eq("0.0.0.0"), anyString()
        );
    }

    @Test
    void testDeductNegativeOrZeroAmount() {
        assertThrows(IllegalArgumentException.class, () -> walletService.deduct(userId, new BigDecimal("-50.00")));
        assertThrows(IllegalArgumentException.class, () -> walletService.deduct(userId, BigDecimal.ZERO));
        
        verifyNoInteractions(userRepository);
    }
}
