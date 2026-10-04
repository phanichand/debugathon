package com.debugathon.problem1.payment;

import com.debugathon.problem1.payment.service.PaymentService;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class PaymentMetricsIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private MeterRegistry meterRegistry;

    private double counterValue(String name) {
        var counter = meterRegistry.find(name).counter();
        return counter == null ? 0.0 : counter.count();
    }

    @Test
    void chargeIncrementsRequestCounterAndRecordsDuration() {
        double before = counterValue("payment_requests_total");

        paymentService.charge("BK-METRICS-1", new BigDecimal("1240.00"));

        assertThat(counterValue("payment_requests_total")).isEqualTo(before + 1);
        assertThat(meterRegistry.find("payment_request_duration_seconds").timer().count()).isGreaterThan(0);
    }
}
