package com.stockmanager.order.dto;

import com.stockmanager.order.CustomerOrder;
import com.stockmanager.order.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderSummaryDto(
        Long id,
        String reference,
        Long customerId,
        String customerName,
        String customerCompany,
        OrderStatus status,
        BigDecimal totalHt,
        BigDecimal vatRate,
        BigDecimal totalVat,
        BigDecimal totalTtc,
        int itemCount,
        String createdBy,
        Instant createdAt,
        Instant confirmedAt,
        Instant processingAt,
        Instant shippedAt,
        Instant deliveredAt,
        Instant cancelledAt
) {
    public static OrderSummaryDto from(CustomerOrder order) {
        return new OrderSummaryDto(
                order.getId(),
                order.getReference(),
                order.getCustomer().getId(),
                order.getCustomer().getName(),
                order.getCustomer().getCompany(),
                order.getStatus(),
                order.getTotalHt(),
                order.getVatRate(),
                order.getTotalVat(),
                order.getTotalTtc(),
                order.getItems().size(),
                order.getCreatedBy(),
                order.getCreatedAt(),
                order.getConfirmedAt(),
                order.getProcessingAt(),
                order.getShippedAt(),
                order.getDeliveredAt(),
                order.getCancelledAt()
        );
    }
}
