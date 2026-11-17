package com.stockmanager.category.dto;

import com.stockmanager.category.Category;

import java.time.Instant;

public record CategoryDto(
        Long id,
        String name,
        String description,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static CategoryDto from(Category category) {
        return new CategoryDto(category.getId(), category.getName(), category.getDescription(),
                category.isActive(), category.getCreatedAt(), category.getUpdatedAt());
    }
}
