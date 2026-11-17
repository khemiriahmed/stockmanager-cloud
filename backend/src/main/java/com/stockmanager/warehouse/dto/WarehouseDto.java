package com.stockmanager.warehouse.dto;

import com.stockmanager.warehouse.Warehouse;

import java.time.Instant;

public record WarehouseDto(
        Long id,
        String name,
        String address,
        String manager,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static WarehouseDto from(Warehouse warehouse) {
        return new WarehouseDto(warehouse.getId(), warehouse.getName(), warehouse.getAddress(),
                warehouse.getManager(), warehouse.isActive(), warehouse.getCreatedAt(), warehouse.getUpdatedAt());
    }
}
