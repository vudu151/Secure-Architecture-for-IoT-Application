package com.smartparking.dto.response;

import com.smartparking.enums.DeviceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceDTO {
    private Long id;
    private String deviceUid;
    private DeviceType deviceType;
    private String location;
    private Boolean isOnline;
    private LocalDateTime lastHeartbeat;
    private String firmwareVersion;
}
