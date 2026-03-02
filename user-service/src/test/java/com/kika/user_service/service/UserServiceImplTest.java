package com.kika.user_service.service;

import com.kika.user_service.dto.UserDto;
import com.kika.user_service.entity.User;
import com.kika.user_service.mapper.UserMapper;
import com.kika.user_service.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void getAll_shouldReturnPagedUsers() {
        Page<User> userPage = new PageImpl<>(List.of(new User()));
        when(userRepository.findAll(any(Pageable.class))).thenReturn(userPage);
        when(userMapper.toPage(userPage)).thenReturn(Page.empty());

        userService.getAll(Pageable.unpaged());

        verify(userRepository).findAll(any(Pageable.class));
        verify(userMapper).toPage(userPage);
    }

    @Test
    void getOne_whenFound_shouldReturnDto() {
        User user = new User(1L, "Name", "email@example.com");
        UserDto dto = new UserDto("Name", "email@example.com");

        when(userRepository.getUserById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toUserDto(user)).thenReturn(dto);

        UserDto result = userService.getOne(1L);

        assertEquals(dto, result);
    }

    @Test
    void getOne_whenNotFound_shouldThrow404() {
        when(userRepository.getUserById(1L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userService.getOne(1L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void getMany_whenIdsValid_shouldReturnList() {
        List<User> users = List.of(new User());
        List<UserDto> dtos = List.of(new UserDto());

        when(userRepository.findAllById(List.of(1L, 2L))).thenReturn(users);
        when(userMapper.toUserDtoList(users)).thenReturn(dtos);

        List<UserDto> result = userService.getMany(List.of(1L, 2L));

        assertEquals(dtos, result);
    }

    @Test
    void getMany_whenIdsEmpty_shouldThrow400() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userService.getMany(List.of()));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void create_whenEmailUnique_shouldSaveAndReturnDto() {
        UserDto dto = new UserDto("Name", "email@example.com");
        User entity = new User();
        User saved = new User(1L, "Name", "email@example.com");
        UserDto savedDto = new UserDto("Name", "email@example.com");

        when(userRepository.existsByEmail(dto.getEmail())).thenReturn(false);
        when(userMapper.toEntity(dto)).thenReturn(entity);
        when(userRepository.save(entity)).thenReturn(saved);
        when(userMapper.toUserDto(saved)).thenReturn(savedDto);

        UserDto result = userService.create(dto);

        assertEquals(savedDto, result);
        verify(userRepository).save(entity);
    }

    @Test
    void create_whenEmailExists_shouldThrow409() {
        UserDto dto = new UserDto("Name", "email@example.com");

        when(userRepository.existsByEmail(dto.getEmail())).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userService.create(dto));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void patch_shouldUpdateAndReturnDto() {
        User existing = new User(1L, "Old", "email@example.com");
        UserDto patchDto = new UserDto("New Name", null);
        UserDto updatedDto = new UserDto("New Name", "email@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        doNothing().when(userMapper).updateUser(existing, patchDto);
        when(userRepository.save(existing)).thenReturn(existing);
        when(userMapper.toUserDto(existing)).thenReturn(updatedDto);

        UserDto result = userService.patch(1L, patchDto);

        assertEquals(updatedDto, result);
        verify(userMapper).updateUser(existing, patchDto);
    }

    @Test
    void delete_shouldCallRepository() {
        userService.delete(1L);
        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteMany_shouldCallBatchDelete() {
        userService.deleteMany(List.of(1L, 2L));
        verify(userRepository).deleteAllByIdInBatch(List.of(1L, 2L));
    }
}