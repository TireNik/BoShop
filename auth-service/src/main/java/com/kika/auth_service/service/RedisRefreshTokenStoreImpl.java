package com.kika.auth_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kika.auth_service.dto.RefreshTokenData;
import com.kika.auth_service.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Component
public class RedisRefreshTokenStoreImpl implements RefreshTokenStore {

    private static final String EMAIL_TO_REFRESH_KEY = "rt:email:";
    private final RedisTemplate<String, String> redisTemplate;
    private final JwtTokenProvider jwt;
    private final ObjectMapper objectMapper;

    @Override
    public void save(String refreshToken, String email) {
        Claims claims = jwt.extractAllClaims(refreshToken);
        RefreshTokenData data = RefreshTokenData.builder()
                .email(email)
                .userId(claims.get("userId", Long.class))
                .role(claims.get("role", String.class))
                .build();

        try {
            String value = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(refreshToken, value, 10_000, TimeUnit.SECONDS);
            redisTemplate.opsForValue().set(EMAIL_TO_REFRESH_KEY + email, refreshToken, 10_000, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize refresh token data", e);
        }
    }

    @Override
    public boolean isValid(String token) {
        return redisTemplate.hasKey(token);
    }

    @Override
    public RefreshTokenData getUserData(String token) {
        String value = redisTemplate.opsForValue().get(token);
        if (value == null) return null;

        try {
            return objectMapper.readValue(value, RefreshTokenData.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @Override
    public String rotate(String oldToken) {
        RefreshTokenData data = getUserData(oldToken);
        if (data == null) return null;

        redisTemplate.delete(oldToken);
        redisTemplate.delete(EMAIL_TO_REFRESH_KEY + data.getEmail());

        String newToken = jwt.createRefreshToken(data.getEmail(), data.getUserId());
        store(newToken, data.getEmail(), 10000);

        return newToken;
    }

    @Override
    public void store(String token, String email, long ttlSeconds) {
        Claims claims = jwt.extractAllClaims(token);

        RefreshTokenData data = RefreshTokenData.builder()
                .email(email)
                .userId(claims.get("userId", Long.class))
                .role(claims.get("role", String.class))
                .build();

        try {
            String value = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(token, value, ttlSeconds, TimeUnit.SECONDS);
            redisTemplate.opsForValue().set(EMAIL_TO_REFRESH_KEY + email, token, ttlSeconds, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize refresh token data", e);
        }
    }

    @Override
    public void delete(String token) {
        RefreshTokenData data = getUserData(token);
        if (data != null) {
            redisTemplate.delete(EMAIL_TO_REFRESH_KEY + data.getEmail());
        }
        redisTemplate.delete(token);
    }

    @Override
    public void deleteByEmail(String email) {
        String refreshToken = redisTemplate.opsForValue().get(EMAIL_TO_REFRESH_KEY + email);
        if (refreshToken != null) {
            redisTemplate.delete(refreshToken);
        }
        redisTemplate.delete(EMAIL_TO_REFRESH_KEY + email);
    }
}
