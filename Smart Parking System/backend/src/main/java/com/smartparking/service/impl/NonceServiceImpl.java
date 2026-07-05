package com.smartparking.service.impl;

import com.smartparking.exception.ReplayAttackException;
import com.smartparking.service.NonceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class NonceServiceImpl implements NonceService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${app.nonce-max-age-seconds:5}")
    private long maxAgeSeconds;

    @Override
    public String generateNonce() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public void validateNonce(String nonce, long timestamp) {
        long currentMillis = System.currentTimeMillis();
        long diffMillis = Math.abs(currentMillis - timestamp);

        // 1. Verify timestamp is not too old/future
        if (diffMillis > maxAgeSeconds * 1000) {
            log.warn("Nonce validation failed: Timestamp deviation too large. Diff: {} ms, Limit: {} ms",
                    diffMillis, maxAgeSeconds * 1000);
            throw new ReplayAttackException("Request timestamp is outside the allowed window");
        }

        // 2. Check and store nonce in Redis (atomic operation)
        String redisKey = "nonce:" + nonce;
        Boolean isAbsent = redisTemplate.opsForValue().setIfAbsent(redisKey, "1", maxAgeSeconds * 2, TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(isAbsent)) {
            log.error("Replay attack detected! Nonce {} has already been used", nonce);
            throw new ReplayAttackException("Replay attack detected: Nonce has already been used");
        }
    }
}
