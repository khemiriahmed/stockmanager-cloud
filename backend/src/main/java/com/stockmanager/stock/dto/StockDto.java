package com.stockmanager.stock.dto;

import com.stockmanager.stock.Stock;

public record StockDto(
        Long id,
        Long productId,
        String productReference,
        String productName,
        Long warehouseId,
        String warehouseName,
        int quantity,
        int reservedQuantity,
        int availableQuantity,
        int alertThreshold,
        boolean lowStock
) {
    public static StockDto from(Stock stock) {
        return new StockDto(
                stock.getId(),
                stock.getProduct().getId(),
                stock.getProduct().getReference(),
                stock.getProduct().getName(),
                stock.getWarehouse().getId(),
                stock.getWarehouse().getName(),
                stock.getQuantity(),
                stock.getReservedQuantity(),
                stock.availableQuantity(),
                stock.getProduct().getAlertThreshold(),
                stock.isLowStock()
        );
    }
}
