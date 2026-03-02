package com.kika.user_service.dto;

import com.kika.user_service.entity.User;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for {@link User}
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
        @Size(max = 255)
        private String name;

        @Size(max = 255)
        private String email;

}