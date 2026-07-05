package com.smartparking.service.impl;

import com.smartparking.dto.response.TransactionDTO;
import com.smartparking.entity.Transaction;
import com.smartparking.entity.User;
import com.smartparking.enums.PaymentMethod;
import com.smartparking.enums.PaymentStatus;
import com.smartparking.exception.InsufficientBalanceException;
import com.smartparking.exception.ResourceNotFoundException;
import com.smartparking.repository.TransactionRepository;
import com.smartparking.repository.UserRepository;
import com.smartparking.service.AuditService;
import com.smartparking.service.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletServiceImpl implements WalletService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final AuditService auditService;

    @Override
    public BigDecimal getBalance(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return user.getBalance();
    }

    @Override
    @Transactional
    public BigDecimal topup(Long userId, BigDecimal amount) {
        log.info("Processing topup for user id: {}, amount: {}", userId, amount);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Topup amount must be greater than zero");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setBalance(user.getBalance().add(amount));
        User savedUser = userRepository.save(user);

        // Generate Transaction record
        Transaction tx = Transaction.builder()
                .user(savedUser)
                .amount(amount)
                .paymentMethod(PaymentMethod.BANK_TRANSFER) // Assume bank transfer for mockup topup
                .paymentStatus(PaymentStatus.COMPLETED)
                .transactionRef("TOPUP_" + System.currentTimeMillis())
                .build();
        transactionRepository.save(tx);

        auditService.log(userId, "WALLET_TOPUP", "Wallet", getClientIp(), 
                "Topped up wallet with " + amount + " VND. New balance: " + savedUser.getBalance() + " VND");

        return savedUser.getBalance();
    }

    @Override
    @Transactional
    public void deduct(Long userId, BigDecimal amount) {
        log.info("Processing wallet deduction for user id: {}, amount: {}", userId, amount);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deduction amount must be greater than zero");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (user.getBalance().compareTo(amount) < 0) {
            auditService.log(userId, "WALLET_DEDUCT_FAILED", "Wallet", getClientIp(), 
                    "Failed wallet deduction of " + amount + " VND due to insufficient balance: " + user.getBalance() + " VND");
            throw new InsufficientBalanceException("Insufficient balance: " + user.getBalance() + " VND. Required: " + amount + " VND");
        }

        user.setBalance(user.getBalance().subtract(amount));
        userRepository.save(user);

        auditService.log(userId, "WALLET_DEDUCT", "Wallet", getClientIp(), 
                "Deducted " + amount + " VND from wallet. New balance: " + user.getBalance() + " VND");
    }

    @Override
    public List<TransactionDTO> getMyTransactions(Long userId) {
        log.info("Fetching transactions for user id: {}", userId);
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private TransactionDTO convertToDTO(Transaction t) {
        return TransactionDTO.builder()
                .id(t.getId())
                .bookingCode(t.getBooking() != null ? t.getBooking().getBookingCode() : null)
                .amount(t.getAmount())
                .paymentMethod(t.getPaymentMethod())
                .paymentStatus(t.getPaymentStatus())
                .transactionRef(t.getTransactionRef())
                .createdAt(t.getCreatedAt())
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
