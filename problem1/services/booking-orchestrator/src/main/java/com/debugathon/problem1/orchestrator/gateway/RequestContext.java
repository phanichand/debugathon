package com.debugathon.problem1.orchestrator.gateway;

public record RequestContext(String bookingReference, String correlationId) {
}
