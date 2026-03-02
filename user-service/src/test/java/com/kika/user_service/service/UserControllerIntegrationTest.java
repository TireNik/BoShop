package com.kika.user_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kika.user_service.dto.UserDto;
import com.kika.user_service.entity.User;
import com.kika.user_service.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    private UserDto userDto;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        userDto = new UserDto();
        userDto.setName("Test User");
        userDto.setEmail("test@example.com");
    }

    @Test
    void create_shouldReturn201AndCreatedUser() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("Test User"));
    }

    @Test
    void getAll_shouldReturnPagedUsers() throws Exception {
        userRepository.save(new User(null, "User1", "u1@example.com"));
        userRepository.save(new User(null, "User2", "u2@example.com"));

        mockMvc.perform(get("/api/v1/users")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    void getOne_whenExists_shouldReturnUser() throws Exception {
        User saved = userRepository.save(new User(null, "Name", "email@example.com"));

        mockMvc.perform(get("/api/v1/users/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("email@example.com"));
    }

    @Test
    void getOne_whenNotExists_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/v1/users/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMany_shouldReturnUsers() throws Exception {
        User u1 = userRepository.save(new User(null, "U1", "u1@example.com"));
        User u2 = userRepository.save(new User(null, "U2", "u2@example.com"));

        mockMvc.perform(get("/api/v1/users/by-ids")
                        .param("ids", u1.getId().toString(), u2.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void patch_shouldUpdateUser() throws Exception {
        User saved = userRepository.save(new User(null, "Old", "email@example.com"));
        UserDto patch = new UserDto();
        patch.setName("New Name");

        mockMvc.perform(patch("/api/v1/users/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patch)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"));
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        User saved = userRepository.save(new User(null, "Name", "email@example.com"));
        mockMvc.perform(delete("/api/v1/users/{id}", saved.getId()))
                .andExpect(status().isNoContent());

        assertFalse(userRepository.existsById(saved.getId()));
    }

    @Test
    void deleteMany_shouldReturn204() throws Exception {
        User u1 = userRepository.save(new User(null, "U1", "u1@example.com"));
        User u2 = userRepository.save(new User(null, "U2", "u2@example.com"));

        mockMvc.perform(delete("/api/v1/users")
                        .param("ids", u1.getId().toString(), u2.getId().toString()))
                .andExpect(status().isNoContent());

        assertFalse(userRepository.existsById(u1.getId()));
        assertFalse(userRepository.existsById(u2.getId()));
    }
}
