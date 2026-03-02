package com.kika.order_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO for {@link com.kika.order_service.entity.OrderItem}
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class OrderItemDto {
    @NotNull
    private String productId;
    @NotNull
    private Integer quantity;
    @NotNull
    private Double price;
}