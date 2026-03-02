package com.kika.order_service.dto;

import com.kika.order_service.entity.Order;
import com.kika.order_service.entity.Status;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

/**
 * DTO for {@link Order}
 */
@AllArgsConstructor
@Getter
public class OrderResponseDto {
    private final Long id;
    @NotNull
    private final Long userId;
    @NotNull
    private final Status status;
    @NotNull
    private final Double totalPrice;
    private final Instant createdAt;
    private final Instant updatedAt;
}