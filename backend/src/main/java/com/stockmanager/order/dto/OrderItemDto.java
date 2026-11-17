package com.stockmanager.order.dto;

import com.stockmanager.order.OrderItem;

import java.math.BigDecimal;

public record OrderItemDto(
        Long id,
        Long productId,
        String productReference,
        String productName,
        Long warehouseId,
        String warehouseName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal
) {
    public static OrderItemDto from(OrderItem item) {
        return new OrderItemDto(
                item.getId(),
                item.getProduct().getId(),
                item.getProductReference(),
                item.getProductName(),
                item.getWarehouse().getId(),
                item.getWarehouse().getName(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getLineTotal()
        );
    }
}
