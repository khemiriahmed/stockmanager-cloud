package com.stockmanager.auth.dto;

import com.stockmanager.user.dto.UserDto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserDto user
) {
}
