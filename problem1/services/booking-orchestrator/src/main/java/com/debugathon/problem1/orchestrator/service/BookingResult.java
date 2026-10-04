package com.debugathon.problem1.orchestrator.service;

import java.math.BigDecimal;

public record BookingResult(
        String bookingReference,
        String status,
        String operatorBookingId,
        BigDecimal amount,
        String tripId,
        String customerId
) {
}
