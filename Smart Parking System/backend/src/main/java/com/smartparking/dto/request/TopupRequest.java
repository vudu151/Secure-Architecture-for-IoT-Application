package com.smartparking.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopupRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1000.00", message = "Minimum topup amount is 1000 VND")
    private BigDecimal amount;
}
