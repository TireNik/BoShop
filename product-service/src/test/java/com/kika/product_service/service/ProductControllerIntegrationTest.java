package com.kika.product_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kika.product_service.dto.ProductRequestDto;
import com.kika.product_service.entity.Product;
import com.kika.product_service.repository.ProductRepository;
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

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    private ProductRequestDto requestDto;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();

        requestDto = new ProductRequestDto();
        requestDto.setName("Test Product");
        requestDto.setDescription("Desc");
        requestDto.setPrice(BigDecimal.valueOf(100.0));
    }

    @Test
    void createProduct_shouldReturn201() throws Exception {
        mockMvc.perform(post("/api/v1/products/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Product"))
                .andExpect(jsonPath("$.published").value(false));
    }

    @Test
    void publishProduct_shouldSetPublished() throws Exception {
        Product saved = productRepository.save(Product.builder()
                .name("Prod")
                .price(BigDecimal.valueOf(100.0))
                .build());

        mockMvc.perform(post("/api/v1/products/admin/publish/{id}", saved.getId())
                        .param("publish", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published").value(true));
    }

    @Test
    void getProduct_shouldReturnDto() throws Exception {
        Product saved = productRepository.save(Product.builder()
                .name("Prod")
                .price(BigDecimal.valueOf(100.0))
                .published(true)
                .build());

        mockMvc.perform(get("/api/v1/products/public/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Prod"));
    }

    @Test
    void updateProduct_shouldUpdateFields() throws Exception {
        Product saved = productRepository.save(Product.builder()
                .name("Old")
                .price(BigDecimal.valueOf(100.0))
                .build());

        ProductRequestDto updateDto = new ProductRequestDto();
        updateDto.setName("New Name");
        updateDto.setPrice(BigDecimal.valueOf(200.0));

        mockMvc.perform(post("/api/v1/products/admin/{id}", saved.getId())
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .param("name", "New Name")
                        .param("price", "200.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"));
    }

    @Test
    void deleteProduct_shouldRemove() throws Exception {
        Product saved = productRepository.save(Product.builder().name("ToDelete").build());

        mockMvc.perform(delete("/api/v1/products/admin/{id}", saved.getId()))
                .andExpect(status().isOk());

        assertFalse(productRepository.existsById(saved.getId()));
    }
}
