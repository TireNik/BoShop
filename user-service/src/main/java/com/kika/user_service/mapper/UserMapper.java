package com.kika.user_service.mapper;

import com.kika.user_service.dto.UserDto;
import com.kika.user_service.entity.User;
import org.mapstruct.*;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {
    User toEntity(UserDto userDto);

    UserDto toUserDto(User user);

    void updateUser(@MappingTarget User user, UserDto userDto);

    List<UserDto> toUserDtoList(List<User> users);

    default Page<UserDto> toPage(Page<User> users) {
        return users.map(this::toUserDto);
    }
}