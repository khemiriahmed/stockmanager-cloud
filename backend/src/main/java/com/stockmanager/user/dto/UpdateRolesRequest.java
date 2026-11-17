package com.stockmanager.user.dto;

import com.stockmanager.role.RoleName;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record UpdateRolesRequest(
        @NotEmpty(message = "Au moins un rôle est obligatoire.")
        Set<RoleName> roles
) {
}
