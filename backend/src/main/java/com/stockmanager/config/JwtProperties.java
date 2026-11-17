package com.stockmanager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        long expirationMinutes,
        String issuer
) {
    public long expirationSeconds() {
        return expirationMinutes * 60;
    }
}
