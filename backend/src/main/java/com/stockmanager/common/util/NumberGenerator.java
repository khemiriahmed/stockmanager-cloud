package com.stockmanager.common.util;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;

@Component
public class NumberGenerator {

    private final JdbcTemplate jdbcTemplate;

    public NumberGenerator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String nextOrderReference() {
        Long sequence = jdbcTemplate.queryForObject("select nextval('order_reference_seq')", Long.class);
        return "ORD-%d-%06d".formatted(LocalDate.now(ZoneOffset.UTC).getYear(), sequence);
    }

    public String nextInvoiceNumber() {
        Long sequence = jdbcTemplate.queryForObject("select nextval('invoice_number_seq')", Long.class);
        return "INV-%d-%06d".formatted(LocalDate.now(ZoneOffset.UTC).getYear(), sequence);
    }
}
