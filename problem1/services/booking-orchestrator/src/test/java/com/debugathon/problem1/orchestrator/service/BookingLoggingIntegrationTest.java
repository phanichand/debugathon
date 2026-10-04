package com.debugathon.problem1.orchestrator.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
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
class BookingLoggingIntegrationTest {

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

    @Autowired
    private BookingOrchestrationService orchestrationService;

    @Test
    void confirmedBookingLogsCorrelationAndBookingIdViaMdcWithoutIdempotencyKey() {
        Logger logger = (Logger) LoggerFactory.getLogger(BookingOrchestrationService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            orchestrationService.createBooking(new CreateBookingCommand(
                    "TRIP-100", "CUSTOMER-21", List.of("Arun Kumar"), new BigDecimal("1240.00")));
        } finally {
            logger.detachAppender(appender);
        }

        ILoggingEvent confirmedEvent = appender.list.stream()
                .filter(event -> event.getFormattedMessage().equals("Booking confirmed"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected a 'Booking confirmed' log event"));

        assertThat(confirmedEvent.getMDCPropertyMap()).containsKey("correlationId");
        assertThat(confirmedEvent.getMDCPropertyMap()).containsKey("bookingId");
        assertThat(confirmedEvent.getMDCPropertyMap()).containsKey("operatorBookingId");
        assertThat(confirmedEvent.getMDCPropertyMap()).doesNotContainKey("idempotencyKey");

        assertThat(MDC.getCopyOfContextMap()).satisfiesAnyOf(
                map -> assertThat(map).isNull(),
                map -> assertThat(map).isEmpty());
    }
}
