package com.debugathon.problem1.orchestrator.client.dto;

import java.math.BigDecimal;

public record PaymentRequest(String bookingReference, BigDecimal amount) {
}
