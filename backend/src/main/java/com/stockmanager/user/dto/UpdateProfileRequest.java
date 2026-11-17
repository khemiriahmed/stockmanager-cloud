package com.stockmanager.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Le prénom est obligatoire.")
        @Size(max = 80, message = "Le prénom ne doit pas dépasser 80 caractères.")
        String firstName,

        @NotBlank(message = "Le nom est obligatoire.")
        @Size(max = 80, message = "Le nom ne doit pas dépasser 80 caractères.")
        String lastName
) {
}
