package com.kika.order_service.controller;

import com.kika.order_service.config.JwtUserContextFilter;
import com.kika.order_service.dto.OrderItemDto;
import com.kika.order_service.dto.OrderRequestDto;
import com.kika.order_service.dto.OrderResponseDto;
import com.kika.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders/public")
@RequiredArgsConstructor
public class OrderControllerPublic {

    private final OrderService orderService;
    private final JwtUserContextFilter jwtUserContextFilter;

    @GetMapping("/{id}")
    public OrderResponseDto getOne(@PathVariable("id") Long id) {
        return orderService.getOne(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponseDto create() {
        OrderRequestDto orderRequestDto = OrderRequestDto.builder()
                .userId(jwtUserContextFilter.getCurrentUserId())
                .build();
        return orderService.create(orderRequestDto);
    }

    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/{id}/add-product")
    public OrderResponseDto addProduct(@PathVariable("id") Long id, @RequestBody OrderItemDto dto) {
        return orderService.addProduct(id, dto);
    }

    @PostMapping("/reserve/{id}")
    @ResponseStatus(HttpStatus.OK)
    public OrderResponseDto reserve(@PathVariable("id") Long id) {
        long userId = jwtUserContextFilter.getCurrentUserId();
        return orderService.reserve(id, userId);
    }

    @PostMapping("/pay/{id}")
    @ResponseStatus(HttpStatus.OK)
    public OrderResponseDto pay(@PathVariable("id") Long id) {
        long userId = jwtUserContextFilter.getCurrentUserId();
        return orderService.pay(id, userId);
    }

    @PostMapping("/cancel/{id}")
    @ResponseStatus(HttpStatus.OK)
    public OrderResponseDto cancelOrder(@PathVariable("id") Long id) {
        long userId = jwtUserContextFilter.getCurrentUserId();
        return orderService.cancelOrder(id, userId);
    }
}
