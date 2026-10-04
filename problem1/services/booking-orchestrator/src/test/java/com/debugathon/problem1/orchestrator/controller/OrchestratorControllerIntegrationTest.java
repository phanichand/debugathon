package com.debugathon.problem1.orchestrator.controller;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrchestratorControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    static WireMockServer paymentServer = new WireMockServer(0);
    static WireMockServer operatorServer = new WireMockServer(0);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        paymentServer.start();
        operatorServer.start();
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("payment.service.base-url", () -> "http://localhost:" + paymentServer.port());
        registry.add("operator.service.base-url", () -> "http://localhost:" + operatorServer.port());
    }

    @BeforeEach
    void stubDownstreamServices() {
        paymentServer.stubFor(post(urlEqualTo("/payments"))
                .willReturn(aResponse().withStatus(201).withHeader("Content-Type", "application/json")
                        .withBody("{\"paymentReference\":\"PAY-000001\",\"status\":\"SUCCESS\"}")));
        operatorServer.stubFor(post(urlEqualTo("/operator/bookings"))
                .willReturn(aResponse().withStatus(201).withHeader("Content-Type", "application/json")
                        .withBody("{\"operatorBookingId\":\"OP-000001\",\"status\":\"CONFIRMED\"}")));
    }

    @AfterEach
    void resetStubs() {
        paymentServer.resetAll();
        operatorServer.resetAll();
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void createThenGetReturnsConfirmedBooking() {
        String body = "{\"tripId\":\"TRIP-100\",\"customerId\":\"CUSTOMER-21\",\"passengers\":[{\"name\":\"Arun Kumar\"}],\"amount\":1240.00}";
        var headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

        ResponseEntity<String> createResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/internal/orchestrator/bookings",
                new org.springframework.http.HttpEntity<>(body, headers), String.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).contains("\"status\":\"CONFIRMED\"");

        String bookingId = createResponse.getBody().split("\"bookingId\":\"")[1].split("\"")[0];

        ResponseEntity<String> getResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/internal/orchestrator/bookings/" + bookingId, String.class);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).contains("\"status\":\"CONFIRMED\"");
    }

    @Test
    void getUnknownBookingReturns404() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/internal/orchestrator/bookings/BK-999999", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
