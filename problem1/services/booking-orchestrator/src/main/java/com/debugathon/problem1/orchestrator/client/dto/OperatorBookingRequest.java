package com.debugathon.problem1.orchestrator.client.dto;

import java.math.BigDecimal;
import java.util.List;

public record OperatorBookingRequest(
        String sourceBookingId,
        String tripId,
        List<OperatorPassengerDto> passengers,
        BigDecimal amount
) {
}
