package com.kika.auth_service.service;

import com.kika.auth_service.dto.*;
import com.kika.auth_service.entity.AuthUser;
import com.kika.auth_service.entity.UserRole;
import com.kika.auth_service.mapper.AuthUserMapper;
import com.kika.auth_service.repository.AuthUserRepository;
import com.kika.auth_service.security.JwtTokenProvider;
import com.kika.avro.UserCreatedEvent;
import io.jsonwebtoken.Claims;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@RequiredArgsConstructor
@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthUserRepository authUserRepository;
    private final AuthUserMapper authUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final KafkaTemplate<String, UserCreatedEvent> kafkaTemplate;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenStore refreshTokenStore;

    @Override
    @Transactional
    public void register(AuthUserDto dto) {
        if (authUserRepository.existsByEmail(dto.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already exists");
        }

        dto.setPassword(passwordEncoder.encode(dto.getPassword()));

        AuthUser authUser = authUserMapper.toEntity(dto);
        authUser.setEnabled(true);
        UserRole role;
        if (dto.getRole() != null && UserRole.isValidRole(dto.getRole())) {
            role = UserRole.valueOf(dto.getRole());
        } else {
            role = UserRole.ROLE_USER;
        }
        authUser.setRole(role);
        authUserRepository.save(authUser);

        UserCreatedEvent userCreatedEvent = UserCreatedEvent.newBuilder()
                .setUserId(authUser.getId())
                .setEmail(authUser.getEmail())
                .setName(dto.getName())
                .build();

        kafkaTemplate.send("user-created", String.valueOf(authUser.getId()), userCreatedEvent);
    }

    @Override
    @Transactional
    public LoginResponseDto login(LoginRequestDto dto) {

        if (dto.getEmail() == null || dto.getPassword() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid credentials");
        }

        AuthUser authUser = authUserRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        if (authUser.getPasswordHash() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Password not set");
        }

        if (!passwordEncoder.matches(dto.getPassword(), authUser.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid password");
        }

        String accessToken = jwtTokenProvider.createAccessToken(
                authUser.getEmail(),
                authUser.getId(),
                authUser.getRole().name()
        );
        String refreshToken = jwtTokenProvider.createRefreshToken(
                authUser.getEmail(),
                authUser.getId()
        );

        refreshTokenStore.save(refreshToken, authUser.getEmail());

        return LoginResponseDto
                .builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .role(authUser.getRole().toString())
                .build();
    }

    @Override
    public TokenResponseDto refresh(String refreshToken) {
        if (!refreshTokenStore.isValid(refreshToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        RefreshTokenData data = refreshTokenStore.getUserData(refreshToken);

        AuthUser authUser = authUserRepository.findByEmail(data.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        String newAccessToken = jwtTokenProvider.createAccessToken(authUser.getEmail(),
                authUser.getId(),
                authUser.getRole().name()
        );
        String newRefreshToken = refreshTokenStore.rotate(refreshToken);

        return new TokenResponseDto(newAccessToken, newRefreshToken);
    }

    @Override
    @Transactional
    public AuthUserDto changeRole(Long id, String role) {
        if (!UserRole.isValidRole(role)) {
            throw new IllegalArgumentException("Invalid role");
        }
        AuthUser authUser = authUserRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        authUser.setRole(UserRole.valueOf(role));
        authUserRepository.save(authUser);

        AuthUserDto authUserDto = authUserMapper.toDto(authUser);
        authUserDto.setRole(authUser.getRole().toString());
        authUserDto.setEmail(authUser.getEmail());
        return authUserDto;
    }

    @Override
    @Transactional
    public void logout(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid token");
        }

        String token = authHeader.substring(7);
        Claims claims = jwtTokenProvider.extractAllClaims(token);
        String email = claims.getSubject();

        log.info("User {} logged out", email);
        refreshTokenStore.delete(email);
    }

    @Override
    public boolean existsByEmail(String email) {
        return authUserRepository.existsByEmail(email);
    }

    @Override
    public AuthUser findByEmail(String email) {
        return authUserRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
