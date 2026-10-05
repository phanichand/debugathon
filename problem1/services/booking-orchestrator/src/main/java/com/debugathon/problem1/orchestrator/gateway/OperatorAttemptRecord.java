package com.debugathon.problem1.orchestrator.gateway;

import com.debugathon.problem1.orchestrator.domain.AttemptOutcome;

import java.time.Instant;

public record OperatorAttemptRecord(int attemptNumber, String idempotencyKey,
                                     Instant requestTimestamp, Instant responseTimestamp,
                                     AttemptOutcome outcome, String errorType) {
}
