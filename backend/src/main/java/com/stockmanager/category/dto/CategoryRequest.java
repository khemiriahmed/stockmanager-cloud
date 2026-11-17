package com.stockmanager.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank(message = "Le nom est obligatoire.")
        @Size(max = 120, message = "Le nom ne doit pas dépasser 120 caractères.")
        String name,

        @Size(max = 2000, message = "La description est trop longue.")
        String description
) {
}
