package com.kika.auth_service.controller;

import com.kika.auth_service.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/user/")
@RequiredArgsConstructor
public class AuthControllerUser {

    private final AuthService authService;

    @PostMapping("/logout/{id}")
    public void logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        authService.logout(authHeader);
    }
}
