package com.debugathon.problem1.orchestrator.gateway;

import com.debugathon.problem1.orchestrator.client.OperatorClient;
import com.debugathon.problem1.orchestrator.client.dto.OperatorPassengerDto;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OperatorRetryExecutorTest {

    private WireMockServer wireMockServer;
    private OperatorRetryExecutor retryExecutor;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(0);
        wireMockServer.start();
        OperatorClient client = new OperatorClient(RestClient.builder(), "http://localhost:" + wireMockServer.port(), 200L);
        RequestMetadataFactory metadataFactory = new RequestMetadataFactory();
        OperatorRequestFactory requestFactory = new OperatorRequestFactory(metadataFactory);
        retryExecutor = new OperatorRetryExecutor(requestFactory, client, 2, new io.micrometer.core.instrument.simple.SimpleMeterRegistry());
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void firstAttemptTimesOutSecondAttemptSucceeds() {
        wireMockServer.stubFor(post(urlEqualTo("/operator/bookings"))
                .inScenario("timeout-then-success")
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withFixedDelay(1000))
                .willSetStateTo("RETRIED"));

        wireMockServer.stubFor(post(urlEqualTo("/operator/bookings"))
                .inScenario("timeout-then-success")
                .whenScenarioStateIs("RETRIED")
                .willReturn(aResponse().withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"operatorBookingId\":\"OP-000001\",\"status\":\"CONFIRMED\"}")));

        RequestContext context = new RequestContext("BK-000001", "corr-1");
        OperatorExecutionResult result = retryExecutor.execute(
                context, "TRIP-100", List.of(new OperatorPassengerDto("Arun Kumar")), new BigDecimal("1240.00"));

        assertThat(result.response().operatorBookingId()).isEqualTo("OP-000001");
        assertThat(result.attempts()).hasSize(2);
        wireMockServer.verify(2, postRequestedFor(urlEqualTo("/operator/bookings")));
    }

    @Test
    void everyAttemptTimesOutThrowsExhaustedRetriesException() {
        wireMockServer.stubFor(post(urlEqualTo("/operator/bookings"))
                .willReturn(aResponse().withFixedDelay(1000)));

        RequestContext context = new RequestContext("BK-000002", "corr-2");

        assertThatThrownBy(() -> retryExecutor.execute(
                context, "TRIP-100", List.of(new OperatorPassengerDto("Arun Kumar")), new BigDecimal("1240.00")))
                .isInstanceOf(OperatorExhaustedRetriesException.class)
                .satisfies(ex -> assertThat(((OperatorExhaustedRetriesException) ex).getAttempts()).hasSize(2));

        wireMockServer.verify(2, postRequestedFor(urlEqualTo("/operator/bookings")));
    }
}
