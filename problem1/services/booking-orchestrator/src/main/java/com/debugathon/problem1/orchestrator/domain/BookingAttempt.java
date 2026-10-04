package com.debugathon.problem1.orchestrator.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "booking_attempts")
@Getter
@Setter
@NoArgsConstructor
public class BookingAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "attempt_type", nullable = false)
    private AttemptType attemptType;

    @Column(name = "request_timestamp", nullable = false)
    private Instant requestTimestamp;

    @Column(name = "response_timestamp")
    private Instant responseTimestamp;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttemptOutcome outcome;

    @Column(name = "error_type")
    private String errorType;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public BookingAttempt(Booking booking, int attemptNumber, AttemptType attemptType,
                           Instant requestTimestamp, Instant responseTimestamp, String idempotencyKey,
                           String correlationId, Integer httpStatus, AttemptOutcome outcome,
                           String errorType, Long durationMs) {
        this.booking = booking;
        this.attemptNumber = attemptNumber;
        this.attemptType = attemptType;
        this.requestTimestamp = requestTimestamp;
        this.responseTimestamp = responseTimestamp;
        this.idempotencyKey = idempotencyKey;
        this.correlationId = correlationId;
        this.httpStatus = httpStatus;
        this.outcome = outcome;
        this.errorType = errorType;
        this.durationMs = durationMs;
        this.createdAt = Instant.now();
    }
}
