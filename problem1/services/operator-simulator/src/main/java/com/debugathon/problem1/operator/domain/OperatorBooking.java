package com.debugathon.problem1.operator.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "operator_bookings")
@Getter
@Setter
@NoArgsConstructor
public class OperatorBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operator_booking_reference", nullable = false, unique = true)
    private String operatorBookingReference;

    @Column(name = "source_booking_id", nullable = false)
    private String sourceBookingId;

    @Column(name = "trip_id", nullable = false)
    private String tripId;

    @Column(name = "passenger_data", nullable = false)
    private String passengerData;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OperatorBookingStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public OperatorBooking(String operatorBookingReference, String sourceBookingId, String tripId,
                            String passengerData, BigDecimal amount, String idempotencyKey,
                            OperatorBookingStatus status) {
        this.operatorBookingReference = operatorBookingReference;
        this.sourceBookingId = sourceBookingId;
        this.tripId = tripId;
        this.passengerData = passengerData;
        this.amount = amount;
        this.idempotencyKey = idempotencyKey;
        this.status = status;
        this.createdAt = Instant.now();
    }
}
