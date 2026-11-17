package com.stockmanager.stock;

import com.stockmanager.common.domain.BaseEntity;
import com.stockmanager.product.Product;
import com.stockmanager.warehouse.Warehouse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "stocks")
public class Stock extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Stock() {
    }

    public Stock(Product product, Warehouse warehouse, int quantity, int reservedQuantity) {
        this.product = product;
        this.warehouse = warehouse;
        this.quantity = quantity;
        this.reservedQuantity = reservedQuantity;
    }

    public int availableQuantity() {
        return quantity - reservedQuantity;
    }

    public boolean isLowStock() {
        return availableQuantity() <= product.getAlertThreshold();
    }

    public Product getProduct() {
        return product;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getReservedQuantity() {
        return reservedQuantity;
    }

    public void setReservedQuantity(int reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }

    public long getVersion() {
        return version;
    }
}
