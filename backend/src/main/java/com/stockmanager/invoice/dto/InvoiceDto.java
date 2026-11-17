package com.stockmanager.invoice.dto;

import com.stockmanager.invoice.Invoice;

import java.math.BigDecimal;
import java.time.Instant;

public record InvoiceDto(
        Long id,
        String number,
        Long orderId,
        String orderReference,
        Long customerId,
        String customerName,
        String customerCompany,
        BigDecimal totalHt,
        BigDecimal vatRate,
        BigDecimal totalVat,
        BigDecimal totalTtc,
        Instant issuedAt,
        String downloadPath
) {
    public static InvoiceDto from(Invoice invoice) {
        return new InvoiceDto(
                invoice.getId(),
                invoice.getNumber(),
                invoice.getOrder().getId(),
                invoice.getOrder().getReference(),
                invoice.getCustomer().getId(),
                invoice.getCustomer().getName(),
                invoice.getCustomer().getCompany(),
                invoice.getTotalHt(),
                invoice.getVatRate(),
                invoice.getTotalVat(),
                invoice.getTotalTtc(),
                invoice.getIssuedAt(),
                "/api/v1/invoices/" + invoice.getId() + "/document"
        );
    }
}
