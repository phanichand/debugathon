package com.debugathon.problem1.operator.scenario;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OperatorLatencyIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("operator.deterministic-timeout-delay-ms", () -> "300");
        registry.add("admin.token", () -> "test-secret-token");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void deterministicTimeoutProfileDelaysCreateByAtLeastTheConfiguredAmount() {
        HttpHeaders scenarioHeaders = new HttpHeaders();
        scenarioHeaders.set("X-Admin-Token", "test-secret-token");
        scenarioHeaders.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForEntity("http://localhost:" + port + "/internal/scenarios/operator",
                new HttpEntity<>("{\"profile\":\"DETERMINISTIC_TIMEOUT\"}", scenarioHeaders), String.class);

        HttpHeaders bookingHeaders = new HttpHeaders();
        bookingHeaders.set("X-Idempotency-Key", "IDEMP-LATENCY-1");
        bookingHeaders.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"sourceBookingId\":\"BK-LATENCY-1\",\"tripId\":\"TRIP-100\",\"passengers\":[{\"name\":\"Arun Kumar\"}],\"amount\":1240.00}";

        long start = System.currentTimeMillis();
        ResponseEntity<String> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/operator/bookings",
                new HttpEntity<>(body, bookingHeaders), String.class);
        long elapsedMs = System.currentTimeMillis() - start;

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(elapsedMs).isGreaterThanOrEqualTo(300);
    }
}
