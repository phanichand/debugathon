package com.debugathon.problem1.orchestrator.controller.dto;

import com.debugathon.problem1.orchestrator.service.BookingResult;

import java.math.BigDecimal;

public record BookingResponse(
        String bookingId,
        String status,
        String operatorBookingId,
        BigDecimal amount,
        String tripId,
        String customerId
) {
    public static BookingResponse from(BookingResult result) {
        return new BookingResponse(result.bookingReference(), result.status(), result.operatorBookingId(),
                result.amount(), result.tripId(), result.customerId());
    }
}
