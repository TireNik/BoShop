package com.kika.inventory_service.service;

import com.kika.inventory_service.dto.InventoryDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

public interface InventoryService {
    InventoryDto getStock(String id);

    InventoryDto updateStock(@Valid InventoryDto dto, String id);

    InventoryDto addStock(String id, @Positive int quantity);

    InventoryDto removeStock(String id, @Positive int quantity);
}
