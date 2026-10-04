package com.debugathon.problem1.orchestrator.service;

import com.debugathon.problem1.orchestrator.repository.BookingAttemptRepository;
import com.debugathon.problem1.orchestrator.repository.BookingRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
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
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class BookingOrchestrationServiceIntegrationTest {

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
    void resetDatabase() {
        // Each test asserts on the total row count via findAll(), so the tables must be
        // isolated per test rather than accumulating across the shared Testcontainers Postgres
        // instance. Attempts must go first: booking_attempts.booking_id has no ON DELETE CASCADE.
        bookingAttemptRepository.deleteAll();
        bookingRepository.deleteAll();
    }

    @BeforeEach
    void stubDownstreamServices() {
        paymentServer.stubFor(post(urlEqualTo("/payments"))
                .willReturn(aResponse().withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"paymentReference\":\"PAY-000001\",\"status\":\"SUCCESS\"}")));

        operatorServer.stubFor(post(urlEqualTo("/operator/bookings"))
                .willReturn(aResponse().withStatus(201)
                        .withHeader("Content-Type", "application/json")
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
    private BookingAttemptRepository bookingAttemptRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Test
    void happyPathReachesConfirmedWithOneOperatorBookingAndTwoAttempts() {
        BookingResult result = orchestrationService.createBooking(new CreateBookingCommand(
                "TRIP-100", "CUSTOMER-21", List.of("Arun Kumar"), new BigDecimal("1240.00")));

        assertThat(result.status()).isEqualTo("CONFIRMED");
        assertThat(result.operatorBookingId()).isEqualTo("OP-000001");
        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("1240.00"));

        assertThat(bookingAttemptRepository.findAll()).hasSize(2);

        operatorServer.verify(1, postRequestedFor(urlEqualTo("/operator/bookings"))
                .withHeader("X-Idempotency-Key", matching(".+")));
    }

    @Test
    void operatorTimeoutOnEveryAttemptMarksBookingFailed() {
        operatorServer.resetAll();
        operatorServer.stubFor(post(urlEqualTo("/operator/bookings"))
                .willReturn(aResponse().withFixedDelay(1000)));

        BookingResult result = orchestrationService.createBooking(new CreateBookingCommand(
                "TRIP-100", "CUSTOMER-21", List.of("Arun Kumar"), new BigDecimal("1240.00")));

        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.operatorBookingId()).isNull();

        assertThat(bookingAttemptRepository.findAll()).hasSize(3); // 1 payment + 2 operator attempts
        operatorServer.verify(2, postRequestedFor(urlEqualTo("/operator/bookings")));
    }

    @Test
    void operatorTimeoutThenRecoveryStillConfirms() {
        operatorServer.resetAll();
        operatorServer.stubFor(post(urlEqualTo("/operator/bookings"))
                .inScenario("timeout-then-success")
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withFixedDelay(1000))
                .willSetStateTo("RETRIED"));
        operatorServer.stubFor(post(urlEqualTo("/operator/bookings"))
                .inScenario("timeout-then-success")
                .whenScenarioStateIs("RETRIED")
                .willReturn(aResponse().withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"operatorBookingId\":\"OP-000002\",\"status\":\"CONFIRMED\"}")));

        BookingResult result = orchestrationService.createBooking(new CreateBookingCommand(
                "TRIP-100", "CUSTOMER-21", List.of("Arun Kumar"), new BigDecimal("1240.00")));

        assertThat(result.status()).isEqualTo("CONFIRMED");
        assertThat(result.operatorBookingId()).isEqualTo("OP-000002");
        assertThat(bookingAttemptRepository.findAll()).hasSize(3); // 1 payment + 2 operator attempts

        operatorServer.verify(2, postRequestedFor(urlEqualTo("/operator/bookings")));
    }
}
