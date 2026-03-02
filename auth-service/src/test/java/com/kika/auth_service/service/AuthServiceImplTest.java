package com.kika.auth_service.service;

import com.kika.auth_service.dto.*;
import com.kika.auth_service.entity.AuthUser;
import com.kika.auth_service.entity.UserRole;
import com.kika.auth_service.mapper.AuthUserMapper;
import com.kika.auth_service.repository.AuthUserRepository;
import com.kika.auth_service.security.JwtTokenProvider;
import com.kika.avro.UserCreatedEvent;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;


@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthUserRepository authUserRepository;

    @Mock
    private AuthUserMapper authUserMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private KafkaTemplate<String, UserCreatedEvent> kafkaTemplate;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    @InjectMocks
    private AuthServiceImpl authService;

    private AuthUserDto userDto;
    private AuthUser authUser;
    private LoginRequestDto loginRequest;

    @BeforeEach
    void setUp() {
        userDto = AuthUserDto.builder()
                .email("test@example.com")
                .password("password123")
                .name("Test User")
                .role("USER")
                .build();

        authUser = AuthUser.builder()
                .id(1L)
                .email("test@example.com")
                .passwordHash("encoded_password")
                .role(UserRole.ROLE_USER)
                .enabled(true)
                .build();

        loginRequest = new LoginRequestDto("test@example.com", "password123");
    }

    @Test
    void register_whenUserDoesNotExist_shouldSaveAndSendEvent() {
        given(authUserRepository.existsByEmail("test@example.com")).willReturn(false);
        given(passwordEncoder.encode("password123")).willReturn("encoded_password");
        given(authUserMapper.toEntity(userDto)).willReturn(authUser);

        authService.register(userDto);

        then(authUserRepository).should().save(argThat(user ->
                user.getEmail().equals("test@example.com") &&
                        user.getPasswordHash().equals("encoded_password") &&
                        user.getRole() == UserRole.ROLE_USER
        ));
        then(kafkaTemplate).should().send(eq("user-created"), eq("1"), argThat(event ->
                event.getUserId() == 1L &&
                        event.getEmail().equals("test@example.com")
        ));
    }

    @Test
    void register_whenUserExists_shouldThrowConflict() {
        given(authUserRepository.existsByEmail("test@example.com")).willReturn(true);

        assertThatThrownBy(() -> authService.register(userDto))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status").isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void login_whenValidCredentials_shouldReturnTokens() {
        given(authUserRepository.findByEmail("test@example.com")).willReturn(Optional.of(authUser));
        given(passwordEncoder.matches("password123", "encoded_password")).willReturn(true);
        given(jwtTokenProvider.createAccessToken(anyString(), anyLong(), anyString())).willReturn("access_token");
        given(jwtTokenProvider.createRefreshToken(anyString(), anyLong())).willReturn("refresh_token");
        willDoNothing().given(refreshTokenStore).save(eq("refresh_token"), eq("test@example.com"));

        LoginResponseDto response = authService.login(loginRequest);

        assertThat(response.getAccessToken()).isEqualTo("access_token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh_token");
        assertThat(response.getRole()).isEqualTo("ROLE_USER");
    }

    @Test
    void login_whenUserNotFound_shouldThrowUnauthorized() {
        given(authUserRepository.findByEmail("test@example.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status").isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_whenInvalidPassword_shouldThrowUnauthorized() {
        given(authUserRepository.findByEmail("test@example.com")).willReturn(Optional.of(authUser));
        given(passwordEncoder.matches("wrong", "encoded_password")).willReturn(false);

        LoginRequestDto wrongLogin = new LoginRequestDto("test@example.com", "wrong");

        assertThatThrownBy(() -> authService.login(wrongLogin))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status").isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_whenNullCredentials_shouldThrowBadRequest() {
        LoginRequestDto nullEmail = new LoginRequestDto(null, "pass");
        LoginRequestDto nullPassword = new LoginRequestDto("email", null);

        assertThatThrownBy(() -> authService.login(nullEmail))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);

        assertThatThrownBy(() -> authService.login(nullPassword))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void refresh_whenValidToken_shouldReturnNewTokens() {
        given(refreshTokenStore.isValid("valid_token")).willReturn(true);
        given(refreshTokenStore.getUserData("valid_token"))
                .willReturn(new RefreshTokenData(1L, "test@example.com", UserRole.ROLE_USER.toString()));
        given(authUserRepository.findByEmail("test@example.com")).willReturn(Optional.of(authUser));
        given(jwtTokenProvider.createAccessToken(anyString(), anyLong(), anyString())).willReturn("new_access");
        given(refreshTokenStore.rotate("valid_token")).willReturn("new_refresh");

        TokenResponseDto response = authService.refresh("valid_token");

        assertThat(response.getAccessToken()).isEqualTo("new_access");
        assertThat(response.getRefreshToken()).isEqualTo("new_refresh");
    }

    @Test
    void refresh_whenInvalidToken_shouldThrowUnauthorized() {
        given(refreshTokenStore.isValid("invalid_token")).willReturn(false);

        assertThatThrownBy(() -> authService.refresh("invalid_token"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status").isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void changeRole_whenValidRole_shouldUpdateAndReturnDto() {
        given(authUserRepository.findById(1L)).willReturn(Optional.of(authUser));
        given(authUserMapper.toDto(authUser)).willReturn(userDto);
        userDto.setRole("ADMIN");

        AuthUserDto result = authService.changeRole(1L, "ROLE_ADMIN");

        then(authUserRepository).should().save(argThat(user -> user.getRole() == UserRole.ROLE_ADMIN));
        assertThat(result.getRole()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void changeRole_whenInvalidRole_shouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> authService.changeRole(1L, "INVALID_ROLE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid role");
    }

    @Test
    void changeRole_whenUserNotFound_shouldThrowNotFound() {
        given(authUserRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.changeRole(999L, "ROLE_ADMIN"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void logout_whenValidHeader_shouldDeleteRefreshToken() {
        Claims claims = mock(Claims.class);
        given(claims.getSubject()).willReturn("test@example.com");
        given(jwtTokenProvider.extractAllClaims("valid_token")).willReturn(claims);
        willDoNothing().given(refreshTokenStore).delete("test@example.com");

        authService.logout("Bearer valid_token");

        then(refreshTokenStore).should().delete("test@example.com");
    }

    @Test
    void logout_whenNullHeader_shouldThrowBadRequest() {
        assertThatThrownBy(() -> authService.logout(null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void logout_whenInvalidHeaderFormat_shouldThrowBadRequest() {
        assertThatThrownBy(() -> authService.logout("invalid_token"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void existsByEmail_shouldReturnRepositoryResult() {
        given(authUserRepository.existsByEmail("test@example.com")).willReturn(true);

        boolean exists = authService.existsByEmail("test@example.com");

        assertThat(exists).isTrue();
        then(authUserRepository).should().existsByEmail("test@example.com");
    }

    @Test
    void findByEmail_whenExists_shouldReturnUser() {
        given(authUserRepository.findByEmail("test@example.com")).willReturn(Optional.of(authUser));

        AuthUser result = authService.findByEmail("test@example.com");

        assertThat(result.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void findByEmail_whenNotFound_shouldThrowNotFound() {
        given(authUserRepository.findByEmail("missing@example.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.findByEmail("missing@example.com"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }
}