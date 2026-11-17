package com.stockmanager.user.dto;

import com.stockmanager.user.User;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

public record UserDto(
        Long id,
        String firstName,
        String lastName,
        String fullName,
        String email,
        boolean active,
        Set<String> roles,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserDto from(User user) {
        return new UserDto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.fullName(),
                user.getEmail(),
                user.isActive(),
                user.roleNames().stream().map(Enum::name).collect(Collectors.toCollection(java.util.LinkedHashSet::new)),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
