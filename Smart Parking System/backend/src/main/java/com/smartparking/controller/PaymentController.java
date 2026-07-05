package com.smartparking.controller;

import com.smartparking.dto.request.TopupRequest;
import com.smartparking.dto.response.ApiResponse;
import com.smartparking.dto.response.TransactionDTO;
import com.smartparking.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final WalletService walletService;

    @GetMapping("/wallet/balance")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<BigDecimal>> getBalance() {
        Long userId = getUserId();
        log.info("Fetching balance for user id: {}", userId);
        BigDecimal balance = walletService.getBalance(userId);
        return ResponseEntity.ok(ApiResponse.success("Wallet balance retrieved successfully", balance));
    }

    @PostMapping("/wallet/topup")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<BigDecimal>> topup(@Valid @RequestBody TopupRequest request) {
        Long userId = getUserId();
        log.info("Processing topup for user id: {}, amount: {}", userId, request.getAmount());
        BigDecimal newBalance = walletService.topup(userId, request.getAmount());
        return ResponseEntity.ok(ApiResponse.success("Wallet topped up successfully", newBalance));
    }

    @GetMapping("/transactions/my")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<List<TransactionDTO>>> getMyTransactions() {
        Long userId = getUserId();
        log.info("Fetching transactions for user id: {}", userId);
        List<TransactionDTO> transactions = walletService.getMyTransactions(userId);
        return ResponseEntity.ok(ApiResponse.success("User transactions retrieved successfully", transactions));
    }

    private Long getUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getCredentials();
    }
}
