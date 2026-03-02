package com.kika.inventory_service.controller;

import com.kika.inventory_service.dto.InventoryDto;
import com.kika.inventory_service.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{id}")
    public InventoryDto getStock(@PathVariable("id") String id) {
        return inventoryService.getStock(id);
    }

    @PostMapping("update/{id}")
    public InventoryDto update(@Valid @RequestBody InventoryDto dto,
                                       @PathVariable("id") String id) {
        return inventoryService.updateStock(dto, id);
    }

    @PutMapping("/add/{id}")
    public InventoryDto addStock(@RequestParam int quantity,
                                 @PathVariable("id") String id) {
        return inventoryService.addStock(id, quantity);
    }

    @PutMapping("/remove/{id}")
    public InventoryDto removeStock(@RequestParam int quantity,
                                    @PathVariable("id") String id) {
        return inventoryService.removeStock(id, quantity);
    }
}
