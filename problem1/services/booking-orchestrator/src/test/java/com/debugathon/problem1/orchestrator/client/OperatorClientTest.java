package com.debugathon.problem1.orchestrator.client;

import com.debugathon.problem1.orchestrator.client.dto.OperatorBookingRequest;
import com.debugathon.problem1.orchestrator.client.dto.OperatorPassengerDto;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OperatorClientTest {

    private WireMockServer wireMockServer;
    private OperatorClient operatorClient;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(0);
        wireMockServer.start();
        operatorClient = new OperatorClient(RestClient.builder(), "http://localhost:" + wireMockServer.port(), 5000L);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void bookSendsIdempotencyKeyAndCorrelationIdHeaders() {
        wireMockServer.stubFor(post(urlEqualTo("/operator/bookings"))
                .withHeader("X-Idempotency-Key", equalTo("BK-000001"))
                .withHeader("X-Correlation-Id", equalTo("corr-1"))
                .willReturn(aResponse().withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"operatorBookingId\":\"OP-000001\",\"status\":\"CONFIRMED\"}")));

        var response = operatorClient.book("BK-000001", "corr-1", new OperatorBookingRequest(
                "BK-000001", "TRIP-100", List.of(new OperatorPassengerDto("Arun Kumar")), new BigDecimal("1240.00")));

        assertThat(response.operatorBookingId()).isEqualTo("OP-000001");
        assertThat(response.status()).isEqualTo("CONFIRMED");
    }

    @Test
    void bookThrowsResourceAccessExceptionWhenDownstreamExceedsTimeout() {
        wireMockServer.stubFor(post(urlEqualTo("/operator/bookings"))
                .willReturn(aResponse().withFixedDelay(1000).withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"operatorBookingId\":\"OP-000001\",\"status\":\"CONFIRMED\"}")));

        OperatorClient shortTimeoutClient = new OperatorClient(
                RestClient.builder(), "http://localhost:" + wireMockServer.port(), 200L);

        assertThatThrownBy(() -> shortTimeoutClient.book("BK-000001", "corr-1", new OperatorBookingRequest(
                "BK-000001", "TRIP-100", List.of(new OperatorPassengerDto("Arun Kumar")), new BigDecimal("1240.00"))))
                .isInstanceOf(ResourceAccessException.class);
    }
}
