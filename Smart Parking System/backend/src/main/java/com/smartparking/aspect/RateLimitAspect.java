package com.smartparking.aspect;

import com.smartparking.dto.request.LoginRequest;
import com.smartparking.exception.TooManyRequestsException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
@Slf4j
public class RateLimitAspect {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    @Before("execution(* com.smartparking.controller.AuthController.login(..)) && args(request)")
    public void rateLimitLogin(LoginRequest request) {
        if (request == null || request.getEmail() == null) {
            return;
        }

        String email = request.getEmail();
        Bucket bucket = cache.computeIfAbsent(email, k -> createNewBucket());

        if (!bucket.tryConsume(1)) {
            log.warn("Rate limit exceeded for login attempt with email: {}", email);
            long nanosToWait = bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill();
            long secondsToWait = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(nanosToWait));
            throw new TooManyRequestsException(
                    "Tài khoản bị tạm khóa do đăng nhập sai quá nhiều lần. Vui lòng thử lại sau " + secondsToWait + " giây.",
                    secondsToWait
            );
        }
    }

    private Bucket createNewBucket() {
        // Tối đa 10 tokens, phục hồi 10 tokens mỗi 5 phút (greedy refill)
        return Bucket.builder()
                .addLimit(Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(5))))
                .build();
    }
}
