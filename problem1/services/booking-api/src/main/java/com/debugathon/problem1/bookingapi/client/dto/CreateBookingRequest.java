package com.debugathon.problem1.bookingapi.client.dto;

import java.math.BigDecimal;
import java.util.List;

public record CreateBookingRequest(
        String tripId,
        String customerId,
        List<PassengerRequest> passengers,
        BigDecimal amount
) {
}
