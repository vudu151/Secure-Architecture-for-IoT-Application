package com.smartparking.service;

import com.smartparking.dto.response.TransactionDTO;
import java.math.BigDecimal;
import java.util.List;

public interface WalletService {
    BigDecimal getBalance(Long userId);
    BigDecimal topup(Long userId, BigDecimal amount);
    void deduct(Long userId, BigDecimal amount);
    List<TransactionDTO> getMyTransactions(Long userId);
}
