package com.smartparking.service.impl;

import com.smartparking.dto.request.LoginRequest;
import com.smartparking.dto.request.RefreshTokenRequest;
import com.smartparking.dto.request.RegisterRequest;
import com.smartparking.dto.response.AuthResponse;
import com.smartparking.entity.User;
import com.smartparking.enums.UserRole;
import com.smartparking.exception.BadRequestException;
import com.smartparking.exception.UnauthorizedException;
import com.smartparking.repository.UserRepository;
import com.smartparking.service.AuditService;
import com.smartparking.service.AuthService;
import com.smartparking.util.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final StringRedisTemplate redisTemplate;
    private final AuditService auditService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getEmail());
        
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists: " + request.getEmail());
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.DRIVER)
                .balance(BigDecimal.ZERO)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(savedUser);
        String refreshToken = jwtService.generateRefreshToken(savedUser);
        long expiresIn = jwtService.getExpirationMs(accessToken);

        auditService.log(savedUser.getId(), "USER_REGISTER", "User", getClientIp(), 
                "User successfully registered with email: " + savedUser.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(expiresIn)
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .role(savedUser.getRole().name())
                .fullName(savedUser.getFullName())
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("User login attempt: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    auditService.log(null, "USER_LOGIN_FAILED", "User", getClientIp(), 
                            "Login failed: Email not found: " + request.getEmail());
                    return new UnauthorizedException("Invalid email or password");
                });

        if (!user.getIsActive()) {
            auditService.log(user.getId(), "USER_LOGIN_FAILED", "User", getClientIp(), 
                    "Login failed: Account deactivated for email: " + request.getEmail());
            throw new UnauthorizedException("Your account is deactivated. Please contact support.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            auditService.log(user.getId(), "USER_LOGIN_FAILED", "User", getClientIp(), 
                    "Login failed: Wrong password for email: " + request.getEmail());
            throw new UnauthorizedException("Invalid email or password");
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        long expiresIn = jwtService.getExpirationMs(accessToken);

        auditService.log(user.getId(), "USER_LOGIN", "User", getClientIp(), 
                "User successfully logged in");

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(expiresIn)
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .fullName(user.getFullName())
                .build();
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        log.info("Refreshing token");
        
        String token = request.getRefreshToken();
        if (!jwtService.validateToken(token)) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        String email = jwtService.extractEmail(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User not found for refresh token"));

        if (!user.getIsActive()) {
            throw new UnauthorizedException("User account is deactivated");
        }

        String accessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);
        long expiresIn = jwtService.getExpirationMs(accessToken);

        auditService.log(user.getId(), "TOKEN_REFRESH", "User", getClientIp(), 
                "Tokens successfully refreshed");

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(expiresIn)
                .build();
    }

    @Override
    public void logout(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (token == null || !jwtService.validateToken(token)) {
            log.warn("Invalid logout request: token is null or invalid");
            return;
        }

        String email = jwtService.extractEmail(token);
        Long userId = jwtService.extractUserId(token);
        long expirationMs = jwtService.getExpirationMs(token);

        if (expirationMs > 0) {
            // Blacklist the token in Redis
            String redisKey = "blacklist:" + token;
            redisTemplate.opsForValue().set(redisKey, "1", expirationMs, TimeUnit.MILLISECONDS);
            log.info("Token blacklisted in Redis for {} ms. Key: {}", expirationMs, redisKey);
        }

        auditService.log(userId, "USER_LOGOUT", "User", getClientIp(), 
                "User successfully logged out: " + email);
    }

    private String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            return ip;
        }
        return "0.0.0.0";
    }
}
