package com.kika.inventory_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kika.inventory_service.dto.InventoryDto;
import com.kika.inventory_service.entity.Inventory;
import com.kika.inventory_service.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class InventoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InventoryRepository inventoryRepository;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    private Inventory existingInventory;

    @BeforeEach
    void setUp() {
        inventoryRepository.deleteAll();

        existingInventory = Inventory.builder()
                .productId("prod-123")
                .available(50)
                .reserved(10)
                .build();

        inventoryRepository.save(existingInventory);
    }

    @Test
    void getStock_existingId_shouldReturnDto() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/{id}", existingInventory.getProductId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value("prod-123"))
                .andExpect(jsonPath("$.available").value(50));
    }

    @Test
    void getStock_nonExistingId_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/{id}", "unknown-prod"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateStock_shouldUpdateAvailable() throws Exception {
        InventoryDto updateDto = InventoryDto.builder()
                .productId("prod-123")
                .available(100)
                .build();

        mockMvc.perform(post("/api/v1/inventory/update/{id}", "prod-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(100));
    }

    @Test
    void addStock_shouldIncreaseAvailable() throws Exception {
        mockMvc.perform(put("/api/v1/inventory/add/{id}", "prod-123")
                        .param("quantity", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(80));
    }

    @Test
    void addStock_negativeQuantity_shouldThrow400() throws Exception {
        mockMvc.perform(put("/api/v1/inventory/add/{id}", "prod-123")
                        .param("quantity", "-10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void removeStock_shouldDecreaseAvailable() throws Exception {
        mockMvc.perform(put("/api/v1/inventory/remove/{id}", "prod-123")
                        .param("quantity", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(30));
    }

    @Test
    void removeStock_nonExistingId_shouldReturn404() throws Exception {
        mockMvc.perform(put("/api/v1/inventory/remove/{id}", "unknown-prod")
                        .param("quantity", "10"))
                .andExpect(status().isNotFound());
    }
}