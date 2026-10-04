package com.debugathon.problem1.orchestrator.client;

import com.debugathon.problem1.orchestrator.client.dto.PaymentRequest;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

class PaymentClientTest {

    private WireMockServer wireMockServer;
    private PaymentClient paymentClient;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(0);
        wireMockServer.start();
        paymentClient = new PaymentClient(RestClient.builder(), "http://localhost:" + wireMockServer.port());
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void chargeSendsBookingReferenceAndAmountAndParsesResponse() {
        wireMockServer.stubFor(post(urlEqualTo("/payments"))
                .withRequestBody(matchingJsonPath("$.bookingReference", equalTo("BK-000001")))
                .withRequestBody(matching(".*\"amount\"\\s*:\\s*1240\\.00.*"))
                .willReturn(aResponse().withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"paymentReference\":\"PAY-000001\",\"status\":\"SUCCESS\"}")));

        var response = paymentClient.charge(new PaymentRequest("BK-000001", new BigDecimal("1240.00")));

        assertThat(response.paymentReference()).isEqualTo("PAY-000001");
        assertThat(response.status()).isEqualTo("SUCCESS");
    }
}
