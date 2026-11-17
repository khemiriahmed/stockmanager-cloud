package com.stockmanager.stock.dto;

import com.stockmanager.stock.MovementType;
import com.stockmanager.stock.StockMovement;

import java.time.Instant;

public record StockMovementDto(
        Long id,
        MovementType type,
        Long productId,
        String productReference,
        String productName,
        Long sourceWarehouseId,
        String sourceWarehouseName,
        Long destinationWarehouseId,
        String destinationWarehouseName,
        int quantity,
        String reason,
        String createdBy,
        Instant createdAt
) {
    public static StockMovementDto from(StockMovement movement) {
        return new StockMovementDto(
                movement.getId(),
                movement.getType(),
                movement.getProduct().getId(),
                movement.getProduct().getReference(),
                movement.getProduct().getName(),
                movement.getSourceWarehouse() != null ? movement.getSourceWarehouse().getId() : null,
                movement.getSourceWarehouse() != null ? movement.getSourceWarehouse().getName() : null,
                movement.getDestinationWarehouse() != null ? movement.getDestinationWarehouse().getId() : null,
                movement.getDestinationWarehouse() != null ? movement.getDestinationWarehouse().getName() : null,
                movement.getQuantity(),
                movement.getReason(),
                movement.getCreatedBy(),
                movement.getCreatedAt()
        );
    }
}
