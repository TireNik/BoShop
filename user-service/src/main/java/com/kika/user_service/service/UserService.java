package com.kika.user_service.service;

import com.kika.user_service.dto.UserDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {
    Page<UserDto> getAll(Pageable pageable);
    UserDto getOne(Long id);
    List<UserDto> getMany(List<Long> ids);
    UserDto create(UserDto dto);
    UserDto patch(Long id, UserDto dto);
    void delete(Long id);
    void deleteMany(List<Long> ids);
}
