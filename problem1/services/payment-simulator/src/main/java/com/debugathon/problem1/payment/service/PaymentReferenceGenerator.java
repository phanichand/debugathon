package com.debugathon.problem1.payment.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentReferenceGenerator {

    private final JdbcTemplate jdbcTemplate;

    public PaymentReferenceGenerator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String next() {
        Long value = jdbcTemplate.queryForObject("SELECT nextval('payment_reference_seq')", Long.class);
        return String.format("PAY-%06d", value);
    }
}
