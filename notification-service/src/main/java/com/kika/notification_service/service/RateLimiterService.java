package com.kika.notification_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final StringRedisTemplate redisTemplate;
    private static final int MAX_PER_MINUTE = 5;

    public boolean isAllowed(Long userId) {
        String key = "rate_limit:notification:" + userId;
        String now = String.valueOf(Instant.now().getEpochSecond() / 60);

        Object value = redisTemplate.opsForHash().get(key, now);

        int current = value == null ? 0 : Integer.parseInt(value.toString());

        if (current >= MAX_PER_MINUTE) {
            return false;
        }

        redisTemplate.opsForHash().increment(key, now, 1);
        redisTemplate.expire(key, 2, TimeUnit.MINUTES);
        return true;
    }
}
