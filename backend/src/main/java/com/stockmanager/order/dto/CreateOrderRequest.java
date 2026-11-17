package com.stockmanager.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record CreateOrderRequest(
        @NotNull(message = "Le client est obligatoire.")
        Long customerId,

        @DecimalMin(value = "0.00", message = "Le taux de TVA ne peut pas être négatif.")
        @DecimalMax(value = "100.00", message = "Le taux de TVA ne peut pas dépasser 100.")
        BigDecimal vatRate,

        @Size(max = 2000, message = "Les notes sont trop longues.")
        String notes,

        @NotEmpty(message = "Au moins une ligne de commande est obligatoire.")
        @Valid
        List<CreateOrderItemRequest> items
) {
}
