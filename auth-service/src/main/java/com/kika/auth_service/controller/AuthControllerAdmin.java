package com.kika.auth_service.controller;

import com.kika.auth_service.dto.AuthUserDto;
import com.kika.auth_service.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/admin")
@RequiredArgsConstructor
public class AuthControllerAdmin {

    private final AuthService authService;

    @GetMapping("/hi")
    public String hi () {
        return "hi";
    }

    @PostMapping("/role/change/{id}")
    public AuthUserDto changeRole(@PathVariable("id") Long id,
                                  @RequestParam("role") String role) {
       return authService.changeRole(id, role);
    }
}
