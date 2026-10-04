package com.debugathon.problem1.orchestrator.gateway;

import com.debugathon.problem1.orchestrator.client.dto.OperatorBookingRequest;

public record OperatorAttemptRequest(String idempotencyKey, String correlationId, OperatorBookingRequest body) {
}
