package com.kika.product_service.service;

import com.kika.avro.ProductCreatedEvent;
import com.kika.product_service.dto.ProductFilterDto;
import com.kika.product_service.dto.ProductRequestDto;
import com.kika.product_service.dto.ProductResponseDto;
import com.kika.product_service.entity.Product;
import com.kika.product_service.mapper.ProductMapper;
import com.kika.product_service.repository.ProductRepository;
import com.kika.product_service.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final ProductSearchRepository productSearchRepository;
    private final Clock clock = Clock.systemUTC();

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    @Transactional
    public ProductResponseDto create(ProductRequestDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("ProductRequestDto is null");
        }
        Product product = productMapper.toProduct(dto);
        product.setPublished(false);
        product.setCreatedAt(Instant.now(clock));
        product.setUpdatedAt(Instant.now(clock));

        Product savedProduct = productRepository.save(product);

        kafkaTemplate.send("product.created", savedProduct.getId(), new ProductCreatedEvent(savedProduct.getId()));

        return productMapper.toProductResponseDto(savedProduct);
    }

    @Override
    public ProductResponseDto getProduct(String id) {
        if (id == null) {
            throw new IllegalArgumentException("Id is null");
        }

        Product product = getProductById(id);

        return productMapper.toProductResponseDto(product);
    }

    @Override
    public Page<ProductResponseDto> searchProduct(ProductFilterDto dto) {
        Page<Product> products = productSearchRepository.search(dto);
        return products.map(productMapper::toProductResponseDto);
    }

    @Override
    @Transactional
    public ProductResponseDto publishProduct(String id, boolean publish) {
        if (id == null) {
            throw new IllegalArgumentException("Id is null");
        }
        Product product = getProductById(id);
        product.setPublished(publish);
        product.setUpdatedAt(Instant.now(clock));
        return productMapper.toProductResponseDto(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponseDto update(String id, ProductRequestDto dto) {
        if (id == null) {
            throw new IllegalArgumentException("Id is null");
        }

        Product product = getProductById(id);
        productMapper.updateProduct(product, dto);
        product.setUpdatedAt(Instant.now(clock));

        return productMapper.toProductResponseDto(productRepository.save(product));
    }

    @Override
    @Transactional
    public void delete(String id) {
        if (id == null) {
            throw new IllegalArgumentException("Id is null");
        }
        productRepository.deleteById(id);
    }

    private Product getProductById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
    }
}
