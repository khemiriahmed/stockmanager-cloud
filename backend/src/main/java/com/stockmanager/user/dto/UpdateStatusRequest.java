package com.stockmanager.user.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull(message = "Le statut est obligatoire.") Boolean active) {
}
