package com.smartparking.repository;

import com.smartparking.entity.SecurityAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface SecurityAuditLogRepository extends JpaRepository<SecurityAuditLog, Long> {

    @Query("SELECT s FROM SecurityAuditLog s WHERE " +
            "(:userId IS NULL OR s.userId = :userId) AND " +
            "(:action IS NULL OR s.action = :action) AND " +
            "(:from IS NULL OR s.createdAt >= :from) AND " +
            "(:to IS NULL OR s.createdAt <= :to)")
    Page<SecurityAuditLog> findByFilters(
            @Param("userId") Long userId,
            @Param("action") String action,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );
}
