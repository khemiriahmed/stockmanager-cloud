package com.stockmanager.supplier.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupplierRequest(
        @NotBlank(message = "Le nom est obligatoire.")
        @Size(max = 150, message = "Le nom ne doit pas dépasser 150 caractères.")
        String name,

        @Size(max = 150, message = "La société ne doit pas dépasser 150 caractères.")
        String company,

        @Email(message = "L'email est invalide.")
        @Size(max = 160, message = "L'email ne doit pas dépasser 160 caractères.")
        String email,

        @Size(max = 40, message = "Le téléphone ne doit pas dépasser 40 caractères.")
        String phone,

        @Size(max = 2000, message = "L'adresse est trop longue.")
        String address
) {
}
