package com.debugathon.problem1.orchestrator.gateway;

import com.debugathon.problem1.orchestrator.client.dto.OperatorPassengerDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OperatorRequestFactoryTest {

    private final RequestMetadataFactory metadataFactory =
            new RequestMetadataFactory(new OutboundRequestIdentityFactory());
    private final OperatorRequestFactory requestFactory = new OperatorRequestFactory(metadataFactory);

    @Test
    void createBuildsRequestBodyFromInputs() {
        RequestContext context = new RequestContext("BK-000001", "corr-1");
        List<OperatorPassengerDto> passengers = List.of(new OperatorPassengerDto("Arun Kumar"));

        OperatorAttemptRequest attemptRequest = requestFactory.create(
                context, "TRIP-100", passengers, new BigDecimal("1240.00"), 1);

        assertThat(attemptRequest.body().sourceBookingId()).isEqualTo("BK-000001");
        assertThat(attemptRequest.body().tripId()).isEqualTo("TRIP-100");
        assertThat(attemptRequest.body().passengers()).isEqualTo(passengers);
        assertThat(attemptRequest.body().amount()).isEqualByComparingTo(new BigDecimal("1240.00"));
        assertThat(attemptRequest.correlationId()).isEqualTo("corr-1");
        assertThat(attemptRequest.idempotencyKey()).isNotBlank();
    }
}
