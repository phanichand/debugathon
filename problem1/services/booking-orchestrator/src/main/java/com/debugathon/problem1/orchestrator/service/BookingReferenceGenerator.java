package com.debugathon.problem1.orchestrator.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class BookingReferenceGenerator {

    private final JdbcTemplate jdbcTemplate;

    public BookingReferenceGenerator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String next() {
        Long value = jdbcTemplate.queryForObject("SELECT nextval('booking_reference_seq')", Long.class);
        return String.format("BK-%06d", value);
    }
}
