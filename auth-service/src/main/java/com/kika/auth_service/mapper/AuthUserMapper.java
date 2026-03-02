package com.kika.auth_service.mapper;

import com.kika.auth_service.dto.AuthUserDto;
import com.kika.auth_service.entity.AuthUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface AuthUserMapper {
    @Mapping(source = "password", target = "passwordHash")
    AuthUser toEntity(AuthUserDto authUserDto);

    AuthUserDto toDto(AuthUser authUser);
}