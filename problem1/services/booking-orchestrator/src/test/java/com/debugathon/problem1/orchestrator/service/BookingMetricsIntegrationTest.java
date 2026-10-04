package com.debugathon.problem1.orchestrator.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class BookingMetricsIntegrationTest {

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
        registry.add("operator.timeout-ms", () -> "300");
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

    @Autowired
    private BookingOrchestrationService orchestrationService;

    @Autowired
    private MeterRegistry meterRegistry;

    private double counterValue(String name) {
        var counter = meterRegistry.find(name).counter();
        return counter == null ? 0.0 : counter.count();
    }

    @Test
    void happyPathIncrementsRequestAndConfirmedCounters() {
        double requestsBefore = counterValue("booking_requests_total");
        double confirmedBefore = counterValue("booking_confirmed_total");

        orchestrationService.createBooking(new CreateBookingCommand(
                "TRIP-100", "CUSTOMER-21", List.of("Arun Kumar"), new BigDecimal("1240.00")));

        assertThat(counterValue("booking_requests_total")).isEqualTo(requestsBefore + 1);
        assertThat(counterValue("booking_confirmed_total")).isEqualTo(confirmedBefore + 1);
        assertThat(meterRegistry.find("booking_request_duration_seconds").timer().count()).isGreaterThan(0);
        assertThat(meterRegistry.find("operator_requests_total").counter().count()).isGreaterThan(0);
    }

    @Test
    void operatorTimeoutOnEveryAttemptIncrementsFailedTimeoutAndRetryCounters() {
        operatorServer.resetAll();
        operatorServer.stubFor(post(urlEqualTo("/operator/bookings"))
                .willReturn(aResponse().withFixedDelay(1000)));

        double failedBefore = counterValue("booking_failed_total");
        double timeoutsBefore = counterValue("operator_timeouts_total");
        double retriesBefore = counterValue("operator_retries_total");

        orchestrationService.createBooking(new CreateBookingCommand(
                "TRIP-100", "CUSTOMER-21", List.of("Arun Kumar"), new BigDecimal("1240.00")));

        assertThat(counterValue("booking_failed_total")).isEqualTo(failedBefore + 1);
        assertThat(counterValue("operator_timeouts_total")).isEqualTo(timeoutsBefore + 2);
        assertThat(counterValue("operator_retries_total")).isEqualTo(retriesBefore + 1);
    }
}
