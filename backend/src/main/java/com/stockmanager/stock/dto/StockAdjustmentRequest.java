package com.stockmanager.stock.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StockAdjustmentRequest(
        @NotNull(message = "Le produit est obligatoire.")
        Long productId,

        @NotNull(message = "L'entrepôt est obligatoire.")
        Long warehouseId,

        @NotNull(message = "La nouvelle quantité est obligatoire.")
        @Min(value = 0, message = "La quantité ne peut pas être négative.")
        Integer newQuantity,

        @NotBlank(message = "Le motif de l'ajustement est obligatoire.")
        @Size(max = 250, message = "Le motif est trop long.")
        String reason
) {
}
