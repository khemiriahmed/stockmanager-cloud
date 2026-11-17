package com.stockmanager.invoice;

import com.stockmanager.common.domain.BaseEntity;
import com.stockmanager.customer.Customer;
import com.stockmanager.order.CustomerOrder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "invoices")
public class Invoice extends BaseEntity {

    @Column(name = "number", nullable = false, unique = true, length = 30)
    private String number;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private CustomerOrder order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "total_ht", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalHt;

    @Column(name = "vat_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal vatRate;

    @Column(name = "total_vat", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalVat;

    @Column(name = "total_ttc", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalTtc;

    @Column(name = "document_key", nullable = false, length = 300)
    private String documentKey;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    protected Invoice() {
    }

    public Invoice(String number, CustomerOrder order, BigDecimal totalHt, BigDecimal vatRate,
                   BigDecimal totalVat, BigDecimal totalTtc, String documentKey) {
        this.number = number;
        this.order = order;
        this.customer = order.getCustomer();
        this.totalHt = totalHt;
        this.vatRate = vatRate;
        this.totalVat = totalVat;
        this.totalTtc = totalTtc;
        this.documentKey = documentKey;
        this.issuedAt = Instant.now();
    }

    public String getNumber() {
        return number;
    }

    public CustomerOrder getOrder() {
        return order;
    }

    public Customer getCustomer() {
        return customer;
    }

    public BigDecimal getTotalHt() {
        return totalHt;
    }

    public BigDecimal getVatRate() {
        return vatRate;
    }

    public BigDecimal getTotalVat() {
        return totalVat;
    }

    public BigDecimal getTotalTtc() {
        return totalTtc;
    }

    public String getDocumentKey() {
        return documentKey;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }
}
