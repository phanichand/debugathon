package com.debugathon.problem1.payment.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@NoArgsConstructor
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_reference", nullable = false)
    private String bookingReference;

    @Column(name = "payment_reference", nullable = false, unique = true)
    private String paymentReference;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public PaymentTransaction(String bookingReference, String paymentReference,
                               BigDecimal amount, PaymentStatus status) {
        this.bookingReference = bookingReference;
        this.paymentReference = paymentReference;
        this.amount = amount;
        this.status = status;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }
}
