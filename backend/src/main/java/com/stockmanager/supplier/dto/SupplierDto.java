package com.stockmanager.supplier.dto;

import com.stockmanager.supplier.Supplier;

import java.time.Instant;

public record SupplierDto(
        Long id,
        String name,
        String company,
        String email,
        String phone,
        String address,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static SupplierDto from(Supplier supplier) {
        return new SupplierDto(supplier.getId(), supplier.getName(), supplier.getCompany(), supplier.getEmail(),
                supplier.getPhone(), supplier.getAddress(), supplier.isActive(),
                supplier.getCreatedAt(), supplier.getUpdatedAt());
    }
}
