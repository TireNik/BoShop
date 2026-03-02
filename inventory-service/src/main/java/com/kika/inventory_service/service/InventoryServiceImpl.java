package com.kika.inventory_service.service;

import com.kika.avro.ProductCreatedEvent;
import com.kika.inventory_service.dto.InventoryDto;
import com.kika.inventory_service.entity.Inventory;
import com.kika.inventory_service.mapper.InventoryMapper;
import com.kika.inventory_service.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryMapper inventoryMapper;

    @Override
    public InventoryDto getStock(String id) {
        Inventory inventory = findInventoryByProductId(id);

        return inventoryMapper.toInventoryDto(inventory);
    }

    @Override
    public InventoryDto updateStock(InventoryDto dto, String id) {
        Inventory inventory = findInventoryByProductId(id);

        inventory.setAvailable(dto.getAvailable());
        inventoryRepository.save(inventory);

        return inventoryMapper.toInventoryDto(inventory);
    }

    @Override
    public InventoryDto addStock(String id, int quantity) {
        if (quantity < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity cannot be negative");
        }
        Inventory inventory = findInventoryByProductId(id);
        inventory.setAvailable(inventory.getAvailable() + quantity);
        inventoryRepository.save(inventory);
        return inventoryMapper.toInventoryDto(inventory);
    }

    @Override
    public InventoryDto removeStock(String id, int quantity) {
        Inventory inventory = findInventoryByProductId(id);
        if (inventory.getAvailable() < quantity) {
            throw new RuntimeException("Not enough stock");
        }
        inventory.setAvailable(inventory.getAvailable() - quantity);
        inventoryRepository.save(inventory);
        return inventoryMapper.toInventoryDto(inventory);
    }

    @KafkaListener(topics = "product.created",
            groupId = "inventory-service-group",
            concurrency = "3")
    @Transactional
    public void handleProductCreatedEvent(ConsumerRecord<String, ProductCreatedEvent> record) {
        ProductCreatedEvent event = record.value();
        String id = event.getProductId().toString();

        inventoryRepository.findByProductId(id)
                .ifPresentOrElse(inv -> {
                        },
                        () -> {
                            Inventory inventory = Inventory.builder()
                                    .productId(id)
                                    .available(0)
                                    .reserved(0)
                                    .build();
                            inventoryRepository.save(inventory);
                            log.info("Inventory created for product: {}", id);
                        }
                );
    }

    private Inventory findInventoryByProductId(String id) {
        return inventoryRepository.findByProductId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory not found"));
    }
}
