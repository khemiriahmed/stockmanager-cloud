package com.stockmanager.customer.dto;

import com.stockmanager.customer.Customer;

import java.time.Instant;

public record CustomerDto(
        Long id,
        String name,
        String company,
        String email,
        String phone,
        String address,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static CustomerDto from(Customer customer) {
        return new CustomerDto(customer.getId(), customer.getName(), customer.getCompany(), customer.getEmail(),
                customer.getPhone(), customer.getAddress(), customer.isActive(),
                customer.getCreatedAt(), customer.getUpdatedAt());
    }
}
