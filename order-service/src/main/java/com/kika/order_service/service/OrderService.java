package com.kika.order_service.service;

import com.kika.order_service.dto.OrderItemDto;
import com.kika.order_service.dto.OrderRequestDto;
import com.kika.order_service.dto.OrderResponseDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;

import java.util.List;

public interface OrderService {

    PagedModel<OrderResponseDto> getAll(Pageable pageable);

    OrderResponseDto getOne(Long id);

    List<OrderResponseDto> getMany(List<Long> ids);

    OrderResponseDto create(OrderRequestDto dto);


    OrderResponseDto addProduct(Long id, OrderItemDto dto);

    OrderResponseDto reserve(Long orderId, Long userId);

    OrderResponseDto pay(Long orderId, Long userId);

    OrderResponseDto cancelOrder(Long orderId, Long userId);


}