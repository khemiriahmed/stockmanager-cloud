package com.stockmanager.product.dto;

import com.stockmanager.product.Product;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductDto(
        Long id,
        String reference,
        String name,
        String description,
        BigDecimal purchasePrice,
        BigDecimal sellingPrice,
        int alertThreshold,
        String imageUrl,
        Long categoryId,
        String categoryName,
        Long supplierId,
        String supplierName,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProductDto from(Product product) {
        return new ProductDto(
                product.getId(),
                product.getReference(),
                product.getName(),
                product.getDescription(),
                product.getPurchasePrice(),
                product.getSellingPrice(),
                product.getAlertThreshold(),
                product.getImageUrl(),
                product.getCategory() != null ? product.getCategory().getId() : null,
                product.getCategory() != null ? product.getCategory().getName() : null,
                product.getSupplier() != null ? product.getSupplier().getId() : null,
                product.getSupplier() != null ? product.getSupplier().getName() : null,
                product.isActive(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
