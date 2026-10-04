package com.debugathon.problem1.orchestrator.service;

import java.math.BigDecimal;
import java.util.List;

public record CreateBookingCommand(
        String tripId,
        String customerId,
        List<String> passengerNames,
        BigDecimal amount
) {
}
