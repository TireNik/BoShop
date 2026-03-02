package com.kika.order_service.dto;

import com.kika.order_service.entity.Order;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for {@link Order}
 */
@AllArgsConstructor
@Getter
@Builder
@NoArgsConstructor
public class OrderRequestDto {
    @NotNull
    private Long userId;
    private Instant createdAt;
    private Instant updatedAt;
}