package com.debugathon.problem1.orchestrator.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_reference", nullable = false, unique = true)
    private String bookingReference;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "trip_id", nullable = false)
    private String tripId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Column(name = "payment_status")
    private String paymentStatus;

    @Column(name = "payment_reference")
    private String paymentReference;

    @Column(name = "operator_booking_id")
    private String operatorBookingId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    public Booking(String bookingReference, String customerId, String tripId,
                    BigDecimal amount, BookingStatus status) {
        this.bookingReference = bookingReference;
        this.customerId = customerId;
        this.tripId = tripId;
        this.amount = amount;
        this.status = status;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void markPaymentSuccess(String paymentReference) {
        this.paymentStatus = "SUCCESS";
        this.paymentReference = paymentReference;
        this.status = BookingStatus.PAYMENT_SUCCESS;
        this.updatedAt = Instant.now();
    }

    public void markConfirmed(String operatorBookingId) {
        this.operatorBookingId = operatorBookingId;
        this.status = BookingStatus.CONFIRMED;
        this.updatedAt = Instant.now();
    }

    public void markOperatorFailed() {
        this.status = BookingStatus.FAILED;
        this.updatedAt = Instant.now();
    }
}
