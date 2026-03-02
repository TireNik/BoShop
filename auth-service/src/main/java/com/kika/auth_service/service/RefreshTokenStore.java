package com.kika.auth_service.service;

import com.kika.auth_service.dto.RefreshTokenData;

public interface RefreshTokenStore {
    boolean isValid(String token);
    RefreshTokenData getUserData(String token);
    String rotate(String token);
    void store(String token, String email, long ttlSeconds);

    void save(String refreshToken, String email);

    void delete(String token);

    void deleteByEmail(String email);
}
