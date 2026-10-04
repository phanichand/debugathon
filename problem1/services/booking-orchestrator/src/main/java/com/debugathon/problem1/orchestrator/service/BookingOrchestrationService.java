package com.debugathon.problem1.orchestrator.service;

import com.debugathon.problem1.orchestrator.client.PaymentClient;
import com.debugathon.problem1.orchestrator.client.dto.OperatorPassengerDto;
import com.debugathon.problem1.orchestrator.client.dto.PaymentRequest;
import com.debugathon.problem1.orchestrator.domain.Booking;
import com.debugathon.problem1.orchestrator.gateway.OperatorExecutionResult;
import com.debugathon.problem1.orchestrator.gateway.OperatorExhaustedRetriesException;
import com.debugathon.problem1.orchestrator.gateway.OperatorGateway;
import com.debugathon.problem1.orchestrator.gateway.RequestContext;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class BookingOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(BookingOrchestrationService.class);

    private final BookingStateStore stateStore;
    private final PaymentClient paymentClient;
    private final OperatorGateway operatorGateway;
    private final MeterRegistry meterRegistry;

    public BookingOrchestrationService(BookingStateStore stateStore, PaymentClient paymentClient,
                                        OperatorGateway operatorGateway, MeterRegistry meterRegistry) {
        this.stateStore = stateStore;
        this.paymentClient = paymentClient;
        this.operatorGateway = operatorGateway;
        this.meterRegistry = meterRegistry;
    }

    public BookingResult createBooking(CreateBookingCommand command) {
        meterRegistry.counter("booking_requests_total").increment();
        long startNanos = System.nanoTime();
        try {
            BookingResult result = doCreateBooking(command);
            if ("CONFIRMED".equals(result.status())) {
                meterRegistry.counter("booking_confirmed_total").increment();
            } else if ("FAILED".equals(result.status())) {
                meterRegistry.counter("booking_failed_total").increment();
            }
            return result;
        } finally {
            meterRegistry.timer("booking_request_duration_seconds")
                    .record(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
        }
    }

    private BookingResult doCreateBooking(CreateBookingCommand command) {
        Booking booking = stateStore.createInitial(command.tripId(), command.customerId(), command.amount());
        String correlationId = UUID.randomUUID().toString();

        MDC.put("correlationId", correlationId);
        MDC.put("bookingId", booking.getBookingReference());
        try {
            Instant paymentRequestStart = Instant.now();
            var paymentResponse = paymentClient.charge(
                    new PaymentRequest(booking.getBookingReference(), command.amount()));
            Instant paymentRequestEnd = Instant.now();

            stateStore.recordPaymentSuccess(booking.getId(), paymentResponse.paymentReference(), correlationId,
                    paymentRequestStart, paymentRequestEnd, paymentRequestEnd.toEpochMilli() - paymentRequestStart.toEpochMilli());

            List<OperatorPassengerDto> passengers = command.passengerNames().stream()
                    .map(OperatorPassengerDto::new)
                    .toList();

            RequestContext context = new RequestContext(booking.getBookingReference(), correlationId);
            try {
                OperatorExecutionResult result = operatorGateway.book(context, command.tripId(), passengers, command.amount());
                MDC.put("operatorBookingId", result.response().operatorBookingId());
                MDC.put("attemptNumber", String.valueOf(result.attempts().size()));
                stateStore.recordOperatorConfirmed(booking.getId(), result.response().operatorBookingId(),
                        correlationId, result.attempts());
                log.info("Booking confirmed");
            } catch (OperatorExhaustedRetriesException exhausted) {
                MDC.put("attemptNumber", String.valueOf(exhausted.getAttempts().size()));
                MDC.put("exceptionType", exhausted.getClass().getSimpleName());
                stateStore.recordOperatorFailed(booking.getId(), correlationId, exhausted.getAttempts());
                log.warn("Booking failed after exhausting operator retries");
            }

            Booking finalBooking = stateStore.findByReference(booking.getBookingReference()).orElseThrow();
            return toResult(finalBooking);
        } finally {
            MDC.clear();
        }
    }

    public Optional<BookingResult> findByReference(String bookingReference) {
        return stateStore.findByReference(bookingReference).map(this::toResult);
    }

    private BookingResult toResult(Booking booking) {
        return new BookingResult(booking.getBookingReference(), booking.getStatus().name(),
                booking.getOperatorBookingId(), booking.getAmount(), booking.getTripId(), booking.getCustomerId());
    }
}
