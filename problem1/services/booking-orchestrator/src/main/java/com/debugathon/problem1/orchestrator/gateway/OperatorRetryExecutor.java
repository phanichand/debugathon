package com.debugathon.problem1.orchestrator.gateway;

import com.debugathon.problem1.orchestrator.client.OperatorClient;
import com.debugathon.problem1.orchestrator.client.dto.OperatorBookingResponse;
import com.debugathon.problem1.orchestrator.client.dto.OperatorPassengerDto;
import com.debugathon.problem1.orchestrator.domain.AttemptOutcome;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class OperatorRetryExecutor {

    private final OperatorRequestFactory requestFactory;
    private final OperatorClient operatorClient;
    private final int maxAttempts;
    private final MeterRegistry meterRegistry;

    public OperatorRetryExecutor(OperatorRequestFactory requestFactory, OperatorClient operatorClient,
                                  @Value("${operator.retry.max-attempts:2}") int maxAttempts,
                                  MeterRegistry meterRegistry) {
        this.requestFactory = requestFactory;
        this.operatorClient = operatorClient;
        this.maxAttempts = maxAttempts;
        this.meterRegistry = meterRegistry;
    }

    public OperatorExecutionResult execute(RequestContext context, String tripId, List<OperatorPassengerDto> passengers,
                                            BigDecimal amount) {
        List<OperatorAttemptRecord> attempts = new ArrayList<>();

        for (int attemptNumber = 1; attemptNumber <= maxAttempts; attemptNumber++) {
            if (attemptNumber > 1) {
                meterRegistry.counter("operator_retries_total").increment();
            }
            OperatorAttemptRequest attemptRequest = requestFactory.create(context, tripId, passengers, amount, attemptNumber);
            Instant requestTimestamp = Instant.now();
            meterRegistry.counter("operator_requests_total").increment();
            try {
                OperatorBookingResponse response = timedBook(attemptRequest);
                Instant responseTimestamp = Instant.now();
                attempts.add(new OperatorAttemptRecord(attemptNumber, attemptRequest.idempotencyKey(),
                        requestTimestamp, responseTimestamp, AttemptOutcome.SUCCESS, null));
                return new OperatorExecutionResult(response, attempts);
            } catch (ResourceAccessException timeout) {
                meterRegistry.counter("operator_timeouts_total").increment();
                Instant responseTimestamp = Instant.now();
                attempts.add(new OperatorAttemptRecord(attemptNumber, attemptRequest.idempotencyKey(),
                        requestTimestamp, responseTimestamp, AttemptOutcome.FAILURE,
                        timeout.getClass().getSimpleName()));
                if (attemptNumber == maxAttempts) {
                    throw new OperatorExhaustedRetriesException(attempts, timeout);
                }
            }
        }
        throw new IllegalStateException("Unreachable: loop must return or throw before exiting");
    }

    private OperatorBookingResponse timedBook(OperatorAttemptRequest attemptRequest) {
        long startNanos = System.nanoTime();
        try {
            return operatorClient.book(attemptRequest.idempotencyKey(), attemptRequest.correlationId(), attemptRequest.body());
        } finally {
            meterRegistry.timer("operator_request_duration_seconds")
                    .record(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
        }
    }
}
