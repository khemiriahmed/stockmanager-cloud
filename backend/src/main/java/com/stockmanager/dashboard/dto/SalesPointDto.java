package com.stockmanager.dashboard.dto;

import java.math.BigDecimal;

public record SalesPointDto(String period, BigDecimal sales, long orders) {
}
