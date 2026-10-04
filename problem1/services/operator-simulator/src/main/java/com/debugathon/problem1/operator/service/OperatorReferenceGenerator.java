package com.debugathon.problem1.operator.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class OperatorReferenceGenerator {

    private final JdbcTemplate jdbcTemplate;

    public OperatorReferenceGenerator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String next() {
        Long value = jdbcTemplate.queryForObject("SELECT nextval('operator_booking_reference_seq')", Long.class);
        return String.format("OP-%06d", value);
    }
}
