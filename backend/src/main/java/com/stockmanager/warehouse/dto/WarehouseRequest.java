package com.stockmanager.warehouse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WarehouseRequest(
        @NotBlank(message = "Le nom est obligatoire.")
        @Size(max = 120, message = "Le nom ne doit pas dépasser 120 caractères.")
        String name,

        @Size(max = 2000, message = "L'adresse est trop longue.")
        String address,

        @Size(max = 150, message = "Le responsable ne doit pas dépasser 150 caractères.")
        String manager
) {
}
