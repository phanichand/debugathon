package com.debugathon.problem1.orchestrator.gateway;

public record RequestMetadata(String idempotencyKey, String correlationId) {
}
