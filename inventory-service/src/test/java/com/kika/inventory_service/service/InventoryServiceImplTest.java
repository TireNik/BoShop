package com.kika.inventory_service.service;

import com.kika.avro.ProductCreatedEvent;
import com.kika.inventory_service.dto.InventoryDto;
import com.kika.inventory_service.entity.Inventory;
import com.kika.inventory_service.mapper.InventoryMapper;
import com.kika.inventory_service.repository.InventoryRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryMapper inventoryMapper;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private Inventory inventory;
    private InventoryDto inventoryDto;

    @BeforeEach
    void setUp() {
        inventory = Inventory.builder()
                .productId("1")
                .available(10)
                .reserved(2)
                .build();

        inventoryDto = InventoryDto.builder()
                .productId("1")
                .available(10)
                .build();
    }

    @Test
    void getStock_shouldReturnInventoryDto() {
        when(inventoryRepository.findByProductId("1")).thenReturn(Optional.of(inventory));
        when(inventoryMapper.toInventoryDto(inventory)).thenReturn(inventoryDto);

        InventoryDto result = inventoryService.getStock("1");

        assertEquals(inventoryDto, result);
        verify(inventoryRepository).findByProductId("1");
    }

    @Test
    void getStock_whenNotFound_shouldThrowException() {
        when(inventoryRepository.findByProductId("999")).thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class,
                        () -> inventoryService.getStock("999"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Inventory not found", exception.getReason());
    }

    @Test
    void updateStock_shouldUpdateAndReturnDto() {
        when(inventoryRepository.findByProductId(inventory.getProductId())).thenReturn(Optional.of(inventory));
        when(inventoryMapper.toInventoryDto(inventory)).thenReturn(inventoryDto);
        when(inventoryRepository.save(inventory)).thenReturn(inventory);

        InventoryDto result = inventoryService.updateStock(inventoryDto, "1");

        assertEquals(inventoryDto, result);
        assertThat(inventory.getAvailable()).isEqualTo(10);
        verify(inventoryRepository).save(inventory);
    }

    @Test
    void addStock_shouldIncreaseAvailable() {
        when(inventoryRepository.findByProductId("1")).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(inventory)).thenReturn(inventory);
        when(inventoryMapper.toInventoryDto(any(Inventory.class))).thenAnswer(invocation -> {
            Inventory inv = invocation.getArgument(0);
            return InventoryDto.builder()
                    .productId(inv.getProductId())
                    .available(inv.getAvailable())
                    .build();
        });
        InventoryDto result = inventoryService.addStock("1", 5);

        assertThat(result.getAvailable()).isEqualTo(15);
    }

    @Test
    void removeStock_shouldDecreaseAvailable() {
        when(inventoryRepository.findByProductId("1")).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(inventory)).thenReturn(inventory);
        when(inventoryMapper.toInventoryDto(any(Inventory.class))).thenAnswer(invocation -> {
            Inventory inv = invocation.getArgument(0);
            return InventoryDto.builder()
                    .productId(inv.getProductId())
                    .available(inv.getAvailable())
                    .build();
        });
        InventoryDto result = inventoryService.removeStock("1", 3);

        assertThat(result.getAvailable()).isEqualTo(7);
        verify(inventoryRepository).save(inventory);
    }

    @Test
    void removeStock_whenNotEnoughStock_shouldThrowException() {
        when(inventoryRepository.findByProductId("1")).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> inventoryService.removeStock("1", 15))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Not enough stock");
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void handleProductCreatedEvent_whenInventoryExists_shouldDoNothing() {
        String productId = UUID.randomUUID().toString();
        ProductCreatedEvent event = ProductCreatedEvent.newBuilder()
                .setProductId(productId)
                .build();

        ConsumerRecord<String, ProductCreatedEvent> record = createConsumerRecord(event, productId);

        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));

        inventoryService.handleProductCreatedEvent(createConsumerRecord(event, productId));

        verify(inventoryRepository, never()).save(any(Inventory.class));
        verify(inventoryRepository).findByProductId(productId);
    }

    @Test
    void handleProductCreatedEvent_whenInventoryNotExists_shouldCreate() {
        String productId = UUID.randomUUID().toString();
        ProductCreatedEvent event = ProductCreatedEvent.newBuilder()
                .setProductId(productId)
                .build();

        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.empty());

        inventoryService.handleProductCreatedEvent(createConsumerRecord(event, productId));

        verify(inventoryRepository).save(argThat(inv ->
                inv.getProductId().equals(productId) &&
                        inv.getAvailable() == 0 &&
                        inv.getReserved() == 0
        ));
    }

    @Test
    void handleProductCreatedEvent_withNullEvent_shouldThrowException() {
        ConsumerRecord<String, ProductCreatedEvent> record = new ConsumerRecord<>("product.created", 0, 0L, "1", null);

        assertThatThrownBy(() -> inventoryService.handleProductCreatedEvent(record))
                .isInstanceOf(NullPointerException.class);
    }

    private ConsumerRecord<String, ProductCreatedEvent> createConsumerRecord(ProductCreatedEvent event, String key) {
        return new ConsumerRecord<>("product.created", 0, 0L, key, event);
    }
}