package com.kika.auth_service.service;

import com.kika.auth_service.dto.AuthUserDto;
import com.kika.auth_service.dto.LoginRequestDto;
import com.kika.auth_service.dto.LoginResponseDto;
import com.kika.auth_service.dto.TokenResponseDto;
import com.kika.auth_service.entity.AuthUser;
import jakarta.validation.Valid;

public interface AuthService {
    void register(@Valid AuthUserDto dto);

    LoginResponseDto login(@Valid LoginRequestDto dto);

    TokenResponseDto refresh(String refreshToken);

    AuthUserDto changeRole(Long id, String role);

    void logout(String authHeader);

    boolean existsByEmail(String email);

    AuthUser findByEmail(String email);
}
