package com.kika.user_service.service;

import com.kika.user_service.dto.UserDto;
import com.kika.user_service.entity.User;
import com.kika.user_service.mapper.UserMapper;
import com.kika.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public Page<UserDto> getAll(Pageable pageable) {
        Page<User> userPage = userRepository.findAll(pageable);
        return userMapper.toPage(userPage);
    }

    @Override
    public UserDto getOne(Long id) {
        if (id == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Id is null"
            );
        }
        User userOptional = userRepository.getUserById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Entity with id `%s` not found".formatted(id)
                ));

        return userMapper.toUserDto(userOptional);
    }

    @Override
    public List<UserDto> getMany(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ids is null or empty"
            );
        }
        return userMapper.toUserDtoList(userRepository.findAllById(ids));
    }


    @Override
    @Transactional
    public UserDto create(UserDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "User with email `%s` already exists".formatted(dto.getEmail())
            );
        }
        User user = userMapper.toEntity(dto);
        return userMapper.toUserDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserDto patch(Long id, UserDto dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        userMapper.updateUser(user, dto);
        return userMapper.toUserDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (id == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Id is null"
            );
        }
        userRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteMany(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ids is null or empty"
            );
        }
        userRepository.deleteAllByIdInBatch(ids);
    }

}
