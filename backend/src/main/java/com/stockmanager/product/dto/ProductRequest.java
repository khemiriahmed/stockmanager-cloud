package com.stockmanager.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank(message = "La référence est obligatoire.")
        @Size(max = 60, message = "La référence ne doit pas dépasser 60 caractères.")
        String reference,

        @NotBlank(message = "Le nom est obligatoire.")
        @Size(max = 180, message = "Le nom ne doit pas dépasser 180 caractères.")
        String name,

        @Size(max = 5000, message = "La description est trop longue.")
        String description,

        @NotNull(message = "Le prix d'achat est obligatoire.")
        @DecimalMin(value = "0.00", message = "Le prix d'achat ne peut pas être négatif.")
        @Digits(integer = 12, fraction = 2, message = "Le prix d'achat est invalide.")
        BigDecimal purchasePrice,

        @NotNull(message = "Le prix de vente est obligatoire.")
        @DecimalMin(value = "0.00", message = "Le prix de vente ne peut pas être négatif.")
        @Digits(integer = 12, fraction = 2, message = "Le prix de vente est invalide.")
        BigDecimal sellingPrice,

        @Min(value = 0, message = "Le seuil d'alerte ne peut pas être négatif.")
        int alertThreshold,

        @Size(max = 500, message = "L'URL de l'image est trop longue.")
        String imageUrl,

        Long categoryId,

        Long supplierId
) {
}
