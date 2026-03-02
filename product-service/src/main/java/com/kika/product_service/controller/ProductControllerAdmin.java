package com.kika.product_service.controller;

import com.kika.product_service.dto.ProductRequestDto;
import com.kika.product_service.dto.ProductResponseDto;
import com.kika.product_service.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products/admin")
@RequiredArgsConstructor
public class ProductControllerAdmin {

    private final ProductService productService;


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponseDto createProduct(@RequestBody ProductRequestDto dto) {
        return productService.create(dto);
    }

    @PostMapping("/publish/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ProductResponseDto publishProduct(@PathVariable("id") String id,
                                             @RequestParam boolean publish) {
        return productService.publishProduct(id, publish);
    }

    @PostMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ProductResponseDto updateProduct(@PathVariable("id") String id,
                                            @ModelAttribute ProductRequestDto dto) {
        return productService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteProduct(@PathVariable("id") String id) {
        productService.delete(id);
    }
}
