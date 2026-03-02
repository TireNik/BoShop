package com.kika.product_service.dto;

import com.kika.product_service.entity.Product;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO for {@link Product}
 */
@Data
public class ProductFilterDto {
    private String name;
    private String category;
    private boolean published;
    private BigDecimal priceFrom;
    private BigDecimal priceTo;


    private Integer page = 0;
    private Integer size = 10;
}