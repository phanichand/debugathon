package com.debugathon.problem1.orchestrator.gateway;

import com.debugathon.problem1.orchestrator.client.dto.OperatorBookingRequest;
import com.debugathon.problem1.orchestrator.client.dto.OperatorPassengerDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class OperatorRequestFactory {

    private final RequestMetadataFactory metadataFactory;

    public OperatorRequestFactory(RequestMetadataFactory metadataFactory) {
        this.metadataFactory = metadataFactory;
    }

    public OperatorAttemptRequest create(RequestContext context, String tripId, List<OperatorPassengerDto> passengers,
                                          BigDecimal amount, int attemptNumber) {
        RequestMetadata metadata = metadataFactory.create(context, attemptNumber);
        OperatorBookingRequest body = new OperatorBookingRequest(context.bookingReference(), tripId, passengers, amount);
        return new OperatorAttemptRequest(metadata.idempotencyKey(), metadata.correlationId(), body);
    }
}
