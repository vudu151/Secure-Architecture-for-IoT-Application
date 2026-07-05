package com.smartparking.util;

import com.smartparking.entity.User;
import com.smartparking.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    
    // A 256-bit key in Base64 (equivalent to 32 bytes)
    private final String testSecret = "dGhpcy1pcy1hLXNlY3JldC1rZXktZm9yLXVuaXQtdGVzdGluZy1vbmx5LW11c3QtYmUtMzItYnl0ZXM=";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", testSecret);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 900000L); // 15 mins
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiration", 604800000L); // 7 days
    }

    @Test
    void testGenerateAndValidateAccessToken() {
        User user = User.builder()
                .id(123L)
                .email("test@smartparking.com")
                .role(UserRole.DRIVER)
                .build();

        String token = jwtService.generateAccessToken(user);
        assertNotNull(token);
        assertTrue(jwtService.validateToken(token));

        assertEquals("test@smartparking.com", jwtService.extractEmail(token));
        assertEquals(UserRole.DRIVER, jwtService.extractRole(token));
        assertEquals(123L, jwtService.extractUserId(token));
        assertTrue(jwtService.getExpirationMs(token) > 0);
    }

    @Test
    void testGenerateAndValidateRefreshToken() {
        User user = User.builder()
                .id(123L)
                .email("test@smartparking.com")
                .role(UserRole.DRIVER)
                .build();

        String token = jwtService.generateRefreshToken(user);
        assertNotNull(token);
        assertTrue(jwtService.validateToken(token));
        assertEquals("test@smartparking.com", jwtService.extractEmail(token));
        
        // Refresh token doesn't contain role/userId claims by default
        assertNull(jwtService.extractUserId(token));
    }

    @Test
    void testValidateInvalidToken() {
        assertFalse(jwtService.validateToken("invalid-token-string"));
        assertFalse(jwtService.validateToken(""));
        assertFalse(jwtService.validateToken(null));
    }

    @Test
    void testTokenExpiration() {
        // Set very short expiration (1 ms)
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 1L);
        
        User user = User.builder()
                .id(123L)
                .email("test@smartparking.com")
                .role(UserRole.DRIVER)
                .build();

        String token = jwtService.generateAccessToken(user);
        
        // Wait 10ms for expiration
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertFalse(jwtService.validateToken(token));
    }
}
