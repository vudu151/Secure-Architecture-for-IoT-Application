package com.smartparking.service;

import com.smartparking.dto.response.AuditLogDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface AuditService {
    void log(Long userId, String action, String resource, String ipAddress, String details);
    Page<AuditLogDTO> getLogs(Long userId, String action, LocalDateTime from, LocalDateTime to, Pageable pageable);
}
