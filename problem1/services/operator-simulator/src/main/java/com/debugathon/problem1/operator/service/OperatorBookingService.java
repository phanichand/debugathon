package com.debugathon.problem1.operator.service;

import com.debugathon.problem1.operator.domain.OperatorBooking;
import com.debugathon.problem1.operator.domain.OperatorBookingStatus;
import com.debugathon.problem1.operator.dto.PassengerDto;
import com.debugathon.problem1.operator.repository.OperatorBookingRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class OperatorBookingService {

    private final OperatorBookingRepository repository;
    private final OperatorReferenceGenerator referenceGenerator;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final MeterRegistry meterRegistry;

    public OperatorBookingService(OperatorBookingRepository repository,
                                   OperatorReferenceGenerator referenceGenerator,
                                   ObjectMapper objectMapper,
                                   PlatformTransactionManager transactionManager,
                                   MeterRegistry meterRegistry) {
        this.repository = repository;
        this.referenceGenerator = referenceGenerator;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.meterRegistry = meterRegistry;
    }

    /**
     * Fast path: check for an existing booking under this key, then fall back to an
     * insert-and-catch-the-unique-constraint-violation strategy for the true concurrent race.
     *
     * <p>Each database operation below runs in its own, separate physical transaction via
     * {@link TransactionTemplate#execute}. This is deliberate, not incidental: once the insert's
     * flush hits the unique constraint violation, Postgres marks that transaction as aborted, and
     * Hibernate's session becomes unusable for further operations within it (attempting to reuse
     * it — e.g. to run the fallback lookup — throws {@code org.hibernate.AssertionFailure: ...
     * don't flush the Session after an exception occurs}). Running the fallback lookup in a brand
     * new transaction, only after the failed transaction has actually been rolled back, avoids that
     * and guarantees every caller — including every loser of the race — gets back a valid result
     * rather than an exception.
     */
    public OperatorBookingResult createOrReuse(String idempotencyKey, String sourceBookingId, String tripId,
                                                List<PassengerDto> passengers, BigDecimal amount) {
        Optional<OperatorBooking> existing = findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            meterRegistry.counter("operator_idempotent_replay_total").increment();
            return toResult(existing.get());
        }
        try {
            OperatorBooking booking = insertNew(idempotencyKey, sourceBookingId, tripId, passengers, amount);
            meterRegistry.counter("operator_bookings_created_total").increment();
            return toResult(booking);
        } catch (DataIntegrityViolationException raceLostToAnotherInsert) {
            OperatorBooking existing2 = findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> raceLostToAnotherInsert);
            meterRegistry.counter("operator_idempotent_replay_total").increment();
            return toResult(existing2);
        }
    }

    private Optional<OperatorBooking> findByIdempotencyKey(String idempotencyKey) {
        return transactionTemplate.execute(status -> repository.findByIdempotencyKey(idempotencyKey));
    }

    private OperatorBooking insertNew(String idempotencyKey, String sourceBookingId, String tripId,
                                       List<PassengerDto> passengers, BigDecimal amount) {
        return transactionTemplate.execute(status -> {
            String passengerData;
            try {
                passengerData = objectMapper.writeValueAsString(passengers);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Unable to serialize passenger data", e);
            }
            OperatorBooking booking = new OperatorBooking(
                    referenceGenerator.next(), sourceBookingId, tripId, passengerData,
                    amount, idempotencyKey, OperatorBookingStatus.CONFIRMED);
            repository.save(booking);
            repository.flush();
            return booking;
        });
    }

    private OperatorBookingResult toResult(OperatorBooking booking) {
        return new OperatorBookingResult(booking.getOperatorBookingReference(), booking.getStatus());
    }

    public record OperatorBookingResult(String operatorBookingReference, OperatorBookingStatus status) {
    }
}
