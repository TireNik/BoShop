package com.kika.auth_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kika.auth_service.dto.*;
import com.kika.auth_service.entity.AuthUser;
import com.kika.auth_service.entity.UserRole;
import com.kika.auth_service.mapper.AuthUserMapper;
import com.kika.auth_service.repository.AuthUserRepository;
import com.kika.auth_service.security.JwtTokenProvider;
import com.kika.avro.UserCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class AuthServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthUserRepository authUserRepository;

    @Autowired
    private AuthUserMapper authUserMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private RefreshTokenStore refreshTokenStore;

    @MockBean
    private KafkaTemplate<String, UserCreatedEvent> kafkaTemplate;

    private AuthUserDto userDto;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        authUserRepository.deleteAll();

        userDto = AuthUserDto.builder()
                .email("testuser@example.com")
                .password("password123")
                .name("Test User")
                .role(UserRole.ROLE_USER.toString())
                .build();
    }

    @Test
    void register_whenValidUser_shouldReturn201() throws Exception {

        mockMvc.perform(post("/auth/public/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isCreated());

        assertTrue(authUserRepository.existsByEmail("testuser@example.com"));
    }

    @Test
    void register_whenUserExists_shouldReturn409() throws Exception {
        registerUser();

        mockMvc.perform(post("/auth/public/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isConflict());


    }

    @Test
    void login_whenValidCredentials_shouldReturnTokens() throws Exception {
        registerUser();

        doNothing().when(refreshTokenStore).save(anyString(), anyString());

        LoginRequestDto loginDto = new LoginRequestDto("testuser@example.com", "password123");

        mockMvc.perform(post("/auth/public/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }

    @Test
    void login_whenInvalidPassword_shouldReturn401() throws Exception {
        registerUser();

        doNothing().when(refreshTokenStore).save(anyString(), anyString());

        LoginRequestDto loginDto = new LoginRequestDto("testuser@example.com", "wrongpass");

        mockMvc.perform(post("/auth/public/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changeRole_whenAdmin_shouldSucceed() throws Exception {
        AuthUser user = registerUser();

        AuthUser admin = AuthUser.builder()
                .email("admin@example.com")
                .passwordHash("encoded")
                .role(UserRole.ROLE_ADMIN)
                .enabled(true)
                .build();

        AuthUser savedAdmin = authUserRepository.save(admin);

        String adminToken = jwtTokenProvider.createAccessToken(
                admin.getEmail(),
                savedAdmin.getId(),
                savedAdmin.getRole().name()
        );

        mockMvc.perform(post("/auth/admin/role/change/{id}", user.getId())
                        .param("role", "ROLE_ADMIN")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", notNullValue()))
                .andExpect(jsonPath("$.role", notNullValue()))
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));
    }

    @Test
    void logout_whenValidToken_shouldDeleteRefreshToken() throws Exception {
        AuthUser user = registerUser();

        LoginRequestDto loginDto = new LoginRequestDto(
                user.getEmail(),
                "password123"
        );

        MvcResult loginResult = mockMvc.perform(post("/auth/public/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.role").value(user.getRole().name()))
                .andReturn();


        String jsonResponse = loginResult.getResponse().getContentAsString();
        LoginResponseDto loginResponseDto = objectMapper.readValue(jsonResponse, LoginResponseDto.class);

        mockMvc.perform(post("/auth/user/logout/{id}", user.getId())
                        .header("Authorization", "Bearer " + loginResponseDto.getAccessToken()))
                .andExpect(status().isOk());

        assertThat(refreshTokenStore.isValid(loginResponseDto.getRefreshToken())).isFalse();
    }

    private AuthUser registerUser() {
        AuthUser user = authUserMapper.toEntity(userDto);
        user.setPasswordHash(passwordEncoder.encode(userDto.getPassword()));

        AuthUser savedUser = authUserRepository.save(user);
        assertTrue(authUserRepository.existsByEmail("testuser@example.com"));

        return savedUser;
    }
}