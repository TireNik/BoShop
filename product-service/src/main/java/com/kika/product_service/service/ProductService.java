package com.kika.product_service.service;

import com.kika.product_service.dto.ProductFilterDto;
import com.kika.product_service.dto.ProductRequestDto;
import com.kika.product_service.dto.ProductResponseDto;
import org.springframework.data.domain.Page;

public interface ProductService {
    ProductResponseDto create(ProductRequestDto dto);

    ProductResponseDto getProduct(String id);

    Page<ProductResponseDto> searchProduct(ProductFilterDto dto);

    ProductResponseDto publishProduct(String id, boolean publish);

    ProductResponseDto update(String id, ProductRequestDto dto);

    void delete(String id);
}

