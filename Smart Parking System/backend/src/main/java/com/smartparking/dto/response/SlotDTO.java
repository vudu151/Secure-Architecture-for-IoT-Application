package com.smartparking.dto.response;

import com.smartparking.enums.SlotStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlotDTO {
    private Long id;
    private String slotCode;
    private String zone;
    private SlotStatus status;
    private String sensorId;
}
