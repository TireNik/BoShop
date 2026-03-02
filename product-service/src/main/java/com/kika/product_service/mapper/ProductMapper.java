package com.kika.product_service.mapper;

import com.kika.product_service.dto.ProductRequestDto;
import com.kika.product_service.dto.ProductResponseDto;
import com.kika.product_service.entity.Product;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ProductMapper {

    Product toProduct(ProductRequestDto productRequestDto);

    ProductResponseDto toProductResponseDto(Product product);

    void updateProduct(@MappingTarget Product product, ProductRequestDto dto);
}