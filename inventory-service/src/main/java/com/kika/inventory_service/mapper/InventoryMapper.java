package com.kika.inventory_service.mapper;

import com.kika.inventory_service.entity.Inventory;
import com.kika.inventory_service.dto.InventoryDto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface InventoryMapper {
    Inventory toEntity(InventoryDto inventoryDto);

    InventoryDto toInventoryDto(Inventory inventory);
}