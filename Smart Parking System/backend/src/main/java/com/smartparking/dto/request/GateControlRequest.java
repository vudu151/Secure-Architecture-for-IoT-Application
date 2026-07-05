package com.smartparking.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GateControlRequest {

    @NotBlank(message = "Action is required (e.g., OPEN, CLOSE)")
    private String action;
}
