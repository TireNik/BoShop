package com.kika.order_service.mapper;

import com.kika.order_service.dto.OrderRequestDto;
import com.kika.order_service.dto.OrderResponseDto;
import com.kika.order_service.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrderMapper {
    Order toEntity(OrderRequestDto orderRequestDto);

    OrderRequestDto toOrderRequestDto(Order order);

    Order updateWithNull(OrderRequestDto orderRequestDto, @MappingTarget Order order);

    Order toEntity(OrderResponseDto orderResponseDto);

    OrderResponseDto toOrderResponseDto(Order order);
}