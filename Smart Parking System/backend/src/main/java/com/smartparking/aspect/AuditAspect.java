package com.smartparking.aspect;

import com.smartparking.dto.request.GateControlRequest;
import com.smartparking.dto.request.LoginRequest;
import com.smartparking.entity.User;
import com.smartparking.repository.UserRepository;
import com.smartparking.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;
    private final UserRepository userRepository;

    private String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }
        HttpServletRequest request = attributes.getRequest();
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getCredentials() instanceof Long) {
            return (Long) authentication.getCredentials();
        }
        return null;
    }

    // 1. Log Successful Login
    @AfterReturning(
            pointcut = "execution(* com.smartparking.controller.AuthController.login(..)) && args(loginRequest)",
            returning = "result"
    )
    public void logLoginSuccess(LoginRequest loginRequest, Object result) {
        if (loginRequest == null) return;
        String email = loginRequest.getEmail();
        userRepository.findByEmail(email).ifPresent(user -> {
            auditService.log(
                    user.getId(),
                    "LOGIN_SUCCESS",
                    "users",
                    getClientIp(),
                    "User logged in successfully: " + email
            );
            log.info("Audit Log: LOGIN_SUCCESS registered for user: {}", email);
        });
    }

    // 2. Log Failed Login
    @AfterThrowing(
            pointcut = "execution(* com.smartparking.controller.AuthController.login(..)) && args(loginRequest)",
            throwing = "ex"
    )
    public void logLoginFailure(LoginRequest loginRequest, Throwable ex) {
        if (loginRequest == null) return;
        String email = loginRequest.getEmail();
        Long userId = userRepository.findByEmail(email).map(User::getId).orElse(null);
        auditService.log(
                userId,
                "LOGIN_FAILED",
                "users",
                getClientIp(),
                "Failed login attempt for email: " + email + ". Reason: " + ex.getMessage()
        );
        log.warn("Audit Log: LOGIN_FAILED registered for email: {}. Reason: {}", email, ex.getMessage());
    }

    // 3. Log Gate Control
    @AfterReturning(
            pointcut = "execution(* com.smartparking.controller.AdminController.controlGate(..)) && args(gateId, controlRequest)"
    )
    public void logGateControl(String gateId, GateControlRequest controlRequest) {
        if (controlRequest == null) return;
        Long adminId = getCurrentUserId();
        auditService.log(
                adminId,
                "GATE_CONTROL",
                "devices",
                getClientIp(),
                "Admin controlled gate: " + gateId + " with action: " + controlRequest.getAction()
        );
        log.info("Audit Log: GATE_CONTROL registered for gate: {} by admin ID: {}", gateId, adminId);
    }

    // 4. Log User Status Toggle
    @AfterReturning(
            pointcut = "execution(* com.smartparking.controller.AdminController.toggleUserActive(..)) && args(userId)"
    )
    public void logUserStatusToggle(Long userId) {
        Long adminId = getCurrentUserId();
        auditService.log(
                adminId,
                "USER_STATUS_TOGGLE",
                "users",
                getClientIp(),
                "Admin toggled active status for user ID: " + userId
        );
        log.info("Audit Log: USER_STATUS_TOGGLE registered for user ID: {} by admin ID: {}", userId, adminId);
    }
}
