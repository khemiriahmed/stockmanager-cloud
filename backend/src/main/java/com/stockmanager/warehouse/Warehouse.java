package com.stockmanager.warehouse;

import com.stockmanager.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "warehouses")
public class Warehouse extends BaseEntity {

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "address", columnDefinition = "text")
    private String address;

    @Column(name = "manager", length = 150)
    private String manager;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected Warehouse() {
    }

    public Warehouse(String name, String address, String manager) {
        this.name = name;
        this.address = address;
        this.manager = manager;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getManager() {
        return manager;
    }

    public void setManager(String manager) {
        this.manager = manager;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
