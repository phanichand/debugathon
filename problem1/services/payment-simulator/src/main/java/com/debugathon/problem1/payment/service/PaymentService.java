package com.debugathon.problem1.payment.service;

import com.debugathon.problem1.payment.domain.PaymentStatus;
import com.debugathon.problem1.payment.domain.PaymentTransaction;
import com.debugathon.problem1.payment.repository.PaymentTransactionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Service
public class PaymentService {

    private final PaymentTransactionRepository repository;
    private final PaymentReferenceGenerator referenceGenerator;
    private final MeterRegistry meterRegistry;

    public PaymentService(PaymentTransactionRepository repository,
                           PaymentReferenceGenerator referenceGenerator,
                           MeterRegistry meterRegistry) {
        this.repository = repository;
        this.referenceGenerator = referenceGenerator;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public PaymentResult charge(String bookingReference, BigDecimal amount) {
        meterRegistry.counter("payment_requests_total").increment();
        long startNanos = System.nanoTime();
        try {
            String paymentReference = referenceGenerator.next();
            PaymentTransaction transaction = new PaymentTransaction(
                    bookingReference, paymentReference, amount, PaymentStatus.SUCCESS);
            repository.save(transaction);
            return new PaymentResult(paymentReference, PaymentStatus.SUCCESS);
        } catch (RuntimeException ex) {
            meterRegistry.counter("payment_errors_total").increment();
            throw ex;
        } finally {
            meterRegistry.timer("payment_request_duration_seconds")
                    .record(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
        }
    }

    public record PaymentResult(String paymentReference, PaymentStatus status) {
    }
}
