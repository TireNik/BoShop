package com.kika.product_service.service;

import com.kika.avro.ProductCreatedEvent;
import com.kika.product_service.dto.ProductRequestDto;
import com.kika.product_service.dto.ProductResponseDto;
import com.kika.product_service.entity.Product;
import com.kika.product_service.mapper.ProductMapper;
import com.kika.product_service.repository.ProductRepository;
import com.kika.product_service.repository.ProductSearchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductSearchRepository productSearchRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void create_shouldSaveAndSendEvent() {
        ProductRequestDto dto = new ProductRequestDto();
        Product entity = new Product();
        entity.setId("1");

        Product saved = new Product();
        saved.setId("1");
        saved.setPublished(false);

        ProductResponseDto responseDto = new ProductResponseDto();

        when(productMapper.toProduct(dto)).thenReturn(entity);
        when(productRepository.save(entity)).thenReturn(saved);
        when(productMapper.toProductResponseDto(saved)).thenReturn(responseDto);

        ProductResponseDto result = productService.create(dto);

        assertEquals(responseDto, result);
        verify(kafkaTemplate).send(eq("product.created"), eq(saved.getId()), any(ProductCreatedEvent.class));
        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
        assertFalse(entity.isPublished());
    }

    @Test
    void getProduct_whenFound_shouldReturnDto() {
        Product product = new Product();
        product.setId("1");
        ProductResponseDto dto = new ProductResponseDto();

        when(productRepository.findById("1")).thenReturn(Optional.of(product));
        when(productMapper.toProductResponseDto(product)).thenReturn(dto);

        ProductResponseDto result = productService.getProduct("1");

        assertEquals(dto, result);
    }

    @Test
    void getProduct_whenNotFound_shouldThrow() {
        when(productRepository.findById("1")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> productService.getProduct("1"));
    }

    @Test
    void publishProduct_shouldUpdatePublished() {
        Product product = new Product();
        product.setId("1");
        product.setPublished(false);
        Product saved = new Product();
        saved.setPublished(true);
        ProductResponseDto dto = new ProductResponseDto();

        when(productRepository.findById("1")).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(saved);
        when(productMapper.toProductResponseDto(saved)).thenReturn(dto);

        ProductResponseDto result = productService.publishProduct("1", true);

        assertTrue(product.isPublished());
        assertEquals(dto, result);
    }

    @Test
    void update_shouldUpdateFields() {
        Product existing = new Product();
        existing.setId("1");
        ProductRequestDto dto = new ProductRequestDto();
        ProductResponseDto responseDto = new ProductResponseDto();

        when(productRepository.findById("1")).thenReturn(Optional.of(existing));
        doNothing().when(productMapper).updateProduct(existing, dto);
        when(productRepository.save(existing)).thenReturn(existing);
        when(productMapper.toProductResponseDto(existing)).thenReturn(responseDto);

        ProductResponseDto result = productService.update("1", dto);

        assertEquals(responseDto, result);
        verify(productMapper).updateProduct(existing, dto);
    }

    @Test
    void delete_shouldCallRepository() {
        productService.delete("1");
        verify(productRepository).deleteById("1");
    }
}