package com.kika.product_service.controller;

import com.kika.product_service.dto.ProductFilterDto;
import com.kika.product_service.dto.ProductRequestDto;
import com.kika.product_service.dto.ProductResponseDto;
import com.kika.product_service.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products/public")
@RequiredArgsConstructor
public class ProductControllerPublic {

    private final ProductService productService;


    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ProductResponseDto getProduct(@PathVariable("id") String id) {
        return productService.getProduct(id);
    }

    @GetMapping("/search")
    @ResponseStatus(HttpStatus.OK)
    public Page<ProductResponseDto> searchProduct(@ModelAttribute ProductFilterDto dto) {
        return productService.searchProduct(dto);
    }

}
