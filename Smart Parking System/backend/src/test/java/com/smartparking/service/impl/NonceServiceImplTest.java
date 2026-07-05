package com.smartparking.service.impl;

import com.smartparking.exception.ReplayAttackException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NonceServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private NonceServiceImpl nonceService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(nonceService, "maxAgeSeconds", 5L);
    }

    @Test
    void testGenerateNonce() {
        String nonce1 = nonceService.generateNonce();
        String nonce2 = nonceService.generateNonce();

        assertNotNull(nonce1);
        assertNotNull(nonce2);
        assertNotEquals(nonce1, nonce2);
        assertEquals(32, nonce1.length()); // Hexadecimal string without dashes
    }

    @Test
    void testValidateNonceSuccess() {
        String nonce = "testnonce123";
        long timestamp = System.currentTimeMillis();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("nonce:" + nonce), eq("1"), eq(10L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);

        assertDoesNotThrow(() -> nonceService.validateNonce(nonce, timestamp));
        verify(valueOperations, times(1))
                .setIfAbsent(eq("nonce:" + nonce), eq("1"), eq(10L), eq(TimeUnit.SECONDS));
    }

    @Test
    void testValidateNonceExpiredTimestamp() {
        String nonce = "testnonce123";
        // 6 seconds ago (limit is 5 seconds)
        long timestamp = System.currentTimeMillis() - 6000;

        assertThrows(ReplayAttackException.class, () -> nonceService.validateNonce(nonce, timestamp));
        // Should not contact Redis if local time check fails
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void testValidateNonceFutureTimestamp() {
        String nonce = "testnonce123";
        // 6 seconds in the future
        long timestamp = System.currentTimeMillis() + 6000;

        assertThrows(ReplayAttackException.class, () -> nonceService.validateNonce(nonce, timestamp));
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void testValidateNonceDuplicateRejected() {
        String nonce = "testnonce123";
        long timestamp = System.currentTimeMillis();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        // Duplicate nonce -> setIfAbsent returns false
        when(valueOperations.setIfAbsent(eq("nonce:" + nonce), eq("1"), eq(10L), eq(TimeUnit.SECONDS)))
                .thenReturn(false);

        assertThrows(ReplayAttackException.class, () -> nonceService.validateNonce(nonce, timestamp));
    }
}
