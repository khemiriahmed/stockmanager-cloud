package com.stockmanager.stock.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StockOperationRequest(
        @NotNull(message = "Le produit est obligatoire.")
        Long productId,

        @NotNull(message = "L'entrepôt est obligatoire.")
        Long warehouseId,

        @NotNull(message = "La quantité est obligatoire.")
        @Min(value = 1, message = "La quantité doit être au minimum de 1.")
        Integer quantity,

        @Size(max = 250, message = "Le motif est trop long.")
        String reason
) {
}
