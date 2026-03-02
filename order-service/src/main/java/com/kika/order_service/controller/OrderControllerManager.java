package com.kika.order_service.controller;

import com.kika.order_service.dto.OrderItemDto;
import com.kika.order_service.dto.OrderResponseDto;
import com.kika.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders/manager")
@RequiredArgsConstructor
public class OrderControllerManager {

    private final OrderService orderService;

    @GetMapping
    public PagedModel<OrderResponseDto> getAll(Pageable pageable) {
        return orderService.getAll(pageable);
    }

    @GetMapping("/{id}")
    public OrderResponseDto getOne(@PathVariable("id") Long id) {
        return orderService.getOne(id);
    }

    @GetMapping("/by-ids")
    public List<OrderResponseDto> getMany(@RequestParam List<Long> ids) {
        return orderService.getMany(ids);
    }

    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/{id}/add-product")
    public OrderResponseDto addProduct(@PathVariable("id") Long id, @RequestBody OrderItemDto dto) {
        return orderService.addProduct(id, dto);
    }

    @PostMapping("/reserve/{id}")
    @ResponseStatus(HttpStatus.OK)
    public OrderResponseDto reserve(@PathVariable("id") Long id,
                                   @RequestParam Long userId) {
        return orderService.reserve(id, userId);
    }

    @PostMapping("/cancel/{id}")
    @ResponseStatus(HttpStatus.OK)
    public OrderResponseDto cancelOrder(@PathVariable("id") Long id,
                                       @RequestParam Long userId) {
        return orderService.cancelOrder(id, userId);
    }
}
