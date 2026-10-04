package com.debugathon.problem1.orchestrator.gateway;

import com.debugathon.problem1.orchestrator.client.dto.OperatorBookingResponse;

import java.util.List;

public record OperatorExecutionResult(OperatorBookingResponse response, List<OperatorAttemptRecord> attempts) {
}
