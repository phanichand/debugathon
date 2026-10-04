package com.debugathon.problem1.bookingapi.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BookingResponse(
        String bookingId,
        String status,
        String operatorBookingId,
        BigDecimal amount,
        String tripId,
        String customerId
) {
}
