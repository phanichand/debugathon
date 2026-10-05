package com.debugathon.problem1.orchestrator.service;

import com.debugathon.problem1.orchestrator.domain.*;
import com.debugathon.problem1.orchestrator.repository.BookingAttemptRepository;
import com.debugathon.problem1.orchestrator.repository.BookingRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class BookingStateStore {

    private final BookingRepository bookingRepository;
    private final BookingAttemptRepository bookingAttemptRepository;
    private final BookingReferenceGenerator referenceGenerator;

    public BookingStateStore(BookingRepository bookingRepository,
                              BookingAttemptRepository bookingAttemptRepository,
                              BookingReferenceGenerator referenceGenerator) {
        this.bookingRepository = bookingRepository;
        this.bookingAttemptRepository = bookingAttemptRepository;
        this.referenceGenerator = referenceGenerator;
    }

    @Transactional
    public Booking createInitial(String tripId, String customerId, BigDecimal amount) {
        Booking booking = new Booking(referenceGenerator.next(), customerId, tripId, amount, BookingStatus.CREATED);
        bookingRepository.save(booking);
        return booking;
    }

    @Transactional
    public void recordPaymentSuccess(Long bookingId, String paymentReference, String correlationId,
                                      Instant requestTimestamp, Instant responseTimestamp, long durationMs) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        booking.markPaymentSuccess(paymentReference);

        BookingAttempt attempt = new BookingAttempt(booking, 1, AttemptType.PAYMENT,
                requestTimestamp, responseTimestamp, null, correlationId, 201, AttemptOutcome.SUCCESS, null, durationMs);
        bookingAttemptRepository.save(attempt);
    }

    @Transactional
    public void recordOperatorConfirmed(Long bookingId, String operatorBookingId, String correlationId,
                                         List<com.debugathon.problem1.orchestrator.gateway.OperatorAttemptRecord> attempts) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        booking.markConfirmed(operatorBookingId);
        saveAttempts(booking, correlationId, attempts);
    }

    @Transactional
    public void recordOperatorFailed(Long bookingId, String correlationId,
                                      List<com.debugathon.problem1.orchestrator.gateway.OperatorAttemptRecord> attempts) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        booking.markOperatorFailed();
        saveAttempts(booking, correlationId, attempts);
    }

    private void saveAttempts(Booking booking, String correlationId,
                              List<com.debugathon.problem1.orchestrator.gateway.OperatorAttemptRecord> attempts) {
        for (var record : attempts) {
            long durationMs = java.time.Duration.between(record.requestTimestamp(), record.responseTimestamp()).toMillis();
            bookingAttemptRepository.save(new BookingAttempt(booking, record.attemptNumber(), AttemptType.OPERATOR,
                    record.requestTimestamp(), record.responseTimestamp(), record.idempotencyKey(), correlationId, null,
                    record.outcome(), record.errorType(), durationMs));
        }
    }

    @Transactional(readOnly = true)
    public Optional<Booking> findByReference(String bookingReference) {
        return bookingRepository.findByBookingReference(bookingReference);
    }
}
