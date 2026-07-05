package com.smartparking.service.impl;

import com.smartparking.dto.response.AuditLogDTO;
import com.smartparking.entity.SecurityAuditLog;
import com.smartparking.repository.SecurityAuditLogRepository;
import com.smartparking.service.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuditServiceImpl implements AuditService {

    @Autowired
    private SecurityAuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public void log(Long userId, String action, String resource, String ipAddress, String details) {
        SecurityAuditLog logEntry = SecurityAuditLog.builder()
                .userId(userId)
                .action(action)
                .resource(resource)
                .ipAddress(ipAddress)
                .details(details)
                .build();
        auditLogRepository.save(logEntry);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogDTO> getLogs(Long userId, String action, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        Page<SecurityAuditLog> logsPage = auditLogRepository.findByFilters(userId, action, from, to, pageable);
        return logsPage.map(this::convertToDTO);
    }

    private AuditLogDTO convertToDTO(SecurityAuditLog logEntry) {
        return AuditLogDTO.builder()
                .id(logEntry.getId())
                .userId(logEntry.getUserId())
                .action(logEntry.getAction())
                .resource(logEntry.getResource())
                .ipAddress(logEntry.getIpAddress())
                .details(logEntry.getDetails())
                .createdAt(logEntry.getCreatedAt())
                .build();
    }
}
