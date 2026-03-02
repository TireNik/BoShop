package com.kika.auth_service.controller;

import com.kika.auth_service.dto.*;
import com.kika.auth_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/public/")
@RequiredArgsConstructor
public class AuthControllerPublic {

    private final AuthService authService;

    @GetMapping("/hi")
    public String hi() {
        return "Hi";
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody AuthUserDto dto) {
        authService.register(dto);
    }

    @PostMapping("/login")
    public LoginResponseDto login(@Valid @RequestBody LoginRequestDto dto) {
        return authService.login(dto);
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.OK)
    public TokenResponseDto refresh(@RequestBody RefreshTokenRequestDto request) {
        return authService.refresh(request.getRefreshToken());
    }

}

