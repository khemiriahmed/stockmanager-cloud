package com.stockmanager.order.dto;

import com.stockmanager.order.CustomerOrder;
import com.stockmanager.order.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderDto(
        Long id,
        String reference,
        Long customerId,
        String customerName,
        String customerCompany,
        String customerEmail,
        String customerAddress,
        OrderStatus status,
        BigDecimal totalHt,
        BigDecimal vatRate,
        BigDecimal totalVat,
        BigDecimal totalTtc,
        String notes,
        String createdBy,
        Instant createdAt,
        Instant updatedAt,
        Instant confirmedAt,
        Instant processingAt,
        Instant shippedAt,
        Instant deliveredAt,
        Instant cancelledAt,
        List<OrderItemDto> items
) {
    public static OrderDto from(CustomerOrder order) {
        return new OrderDto(
                order.getId(),
                order.getReference(),
                order.getCustomer().getId(),
                order.getCustomer().getName(),
                order.getCustomer().getCompany(),
                order.getCustomer().getEmail(),
                order.getCustomer().getAddress(),
                order.getStatus(),
                order.getTotalHt(),
                order.getVatRate(),
                order.getTotalVat(),
                order.getTotalTtc(),
                order.getNotes(),
                order.getCreatedBy(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getConfirmedAt(),
                order.getProcessingAt(),
                order.getShippedAt(),
                order.getDeliveredAt(),
                order.getCancelledAt(),
                order.getItems().stream().map(OrderItemDto::from).toList()
        );
    }
}
