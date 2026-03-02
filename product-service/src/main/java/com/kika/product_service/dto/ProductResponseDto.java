package com.kika.product_service.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponseDto {
    private String id;
    private String name;
    private String description;
    private BigDecimal price;
    private String category;
    private boolean published;
    private List<String> tags;
    private List<String> images;
    private Instant createdAt;
    private Instant updatedAt;
}