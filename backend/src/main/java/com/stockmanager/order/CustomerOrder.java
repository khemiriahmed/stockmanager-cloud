package com.stockmanager.order;

import com.stockmanager.common.domain.BaseEntity;
import com.stockmanager.customer.Customer;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customer_orders")
public class CustomerOrder extends BaseEntity {

    @Column(name = "reference", nullable = false, unique = true, length = 30)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status = OrderStatus.DRAFT;

    @Column(name = "total_ht", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalHt = BigDecimal.ZERO;

    @Column(name = "vat_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal vatRate = new BigDecimal("20.00");

    @Column(name = "total_vat", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalVat = BigDecimal.ZERO;

    @Column(name = "total_ttc", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalTtc = BigDecimal.ZERO;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "created_by", length = 160)
    private String createdBy;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "processing_at")
    private Instant processingAt;

    @Column(name = "shipped_at")
    private Instant shippedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    protected CustomerOrder() {
    }

    public CustomerOrder(String reference, Customer customer, BigDecimal vatRate, String notes, String createdBy) {
        this.reference = reference;
        this.customer = customer;
        this.vatRate = vatRate;
        this.notes = notes;
        this.createdBy = createdBy;
    }

    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
    }

    public void recalculateTotals() {
        BigDecimal ht = items.stream()
                .map(OrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal vat = ht.multiply(vatRate)
                .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
        this.totalHt = ht.setScale(2, java.math.RoundingMode.HALF_UP);
        this.totalVat = vat;
        this.totalTtc = ht.add(vat).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    public String getReference() {
        return reference;
    }

    public Customer getCustomer() {
        return customer;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
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

    public String getNotes() {
        return notes;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public Instant getProcessingAt() {
        return processingAt;
    }

    public void setProcessingAt(Instant processingAt) {
        this.processingAt = processingAt;
    }

    public Instant getShippedAt() {
        return shippedAt;
    }

    public void setShippedAt(Instant shippedAt) {
        this.shippedAt = shippedAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(Instant deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public List<OrderItem> getItems() {
        return items;
    }
}
