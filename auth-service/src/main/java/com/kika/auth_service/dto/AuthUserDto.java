package com.kika.auth_service.dto;

import lombok.*;

/**
 * DTO for {@link com.kika.auth_service.entity.AuthUser}
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class AuthUserDto {
    private String email;
    private String password;

    private String name;

    private String role;
}