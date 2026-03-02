package com.kika.inventory_service.dto;

import com.kika.inventory_service.entity.Inventory;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * DTO for {@link Inventory}
 */
@AllArgsConstructor
@Getter
@Builder
public class InventoryDto {
    @NotNull
    private final String productId;
    private final Integer available;
}