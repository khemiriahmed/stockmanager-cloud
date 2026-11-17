package com.stockmanager.stock;

public record StockLine(Long productId, Long warehouseId, int quantity) {
}
