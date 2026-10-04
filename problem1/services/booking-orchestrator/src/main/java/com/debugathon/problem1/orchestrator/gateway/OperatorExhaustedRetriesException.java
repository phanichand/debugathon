package com.debugathon.problem1.orchestrator.gateway;

import java.util.List;

public class OperatorExhaustedRetriesException extends RuntimeException {

    private final List<OperatorAttemptRecord> attempts;

    public OperatorExhaustedRetriesException(List<OperatorAttemptRecord> attempts, Throwable cause) {
        super("Operator booking failed after " + attempts.size() + " attempt(s)", cause);
        this.attempts = attempts;
    }

    public List<OperatorAttemptRecord> getAttempts() {
        return attempts;
    }
}
