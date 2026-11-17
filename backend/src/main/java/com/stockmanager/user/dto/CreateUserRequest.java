package com.stockmanager.user.dto;

import com.stockmanager.role.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateUserRequest(
        @NotBlank(message = "Le prénom est obligatoire.")
        @Size(max = 80, message = "Le prénom ne doit pas dépasser 80 caractères.")
        String firstName,

        @NotBlank(message = "Le nom est obligatoire.")
        @Size(max = 80, message = "Le nom ne doit pas dépasser 80 caractères.")
        String lastName,

        @NotBlank(message = "L'email est obligatoire.")
        @Email(message = "L'email est invalide.")
        @Size(max = 160, message = "L'email ne doit pas dépasser 160 caractères.")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire.")
        @Size(min = 8, max = 100, message = "Le mot de passe doit contenir entre 8 et 100 caractères.")
        String password,

        @NotEmpty(message = "Au moins un rôle est obligatoire.")
        Set<RoleName> roles
) {
}
