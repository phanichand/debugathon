package com.debugathon.problem1.operator;

import com.debugathon.problem1.operator.dto.PassengerDto;
import com.debugathon.problem1.operator.service.OperatorBookingService;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class OperatorMetricsIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private OperatorBookingService operatorBookingService;

    @Autowired
    private MeterRegistry meterRegistry;

    private double counterValue(String name) {
        var counter = meterRegistry.find(name).counter();
        return counter == null ? 0.0 : counter.count();
    }

    @Test
    void newBookingIncrementsCreatedCounterNotReplayCounter() {
        double createdBefore = counterValue("operator_bookings_created_total");
        double replayBefore = counterValue("operator_idempotent_replay_total");

        operatorBookingService.createOrReuse("IDEMP-METRICS-1", "BK-METRICS-1", "TRIP-100",
                List.of(new PassengerDto("Arun Kumar")), new BigDecimal("1240.00"));

        assertThat(counterValue("operator_bookings_created_total")).isEqualTo(createdBefore + 1);
        assertThat(counterValue("operator_idempotent_replay_total")).isEqualTo(replayBefore);
    }

    @Test
    void repeatedKeyIncrementsReplayCounterNotCreatedCounter() {
        operatorBookingService.createOrReuse("IDEMP-METRICS-2", "BK-METRICS-2", "TRIP-100",
                List.of(new PassengerDto("Arun Kumar")), new BigDecimal("1240.00"));

        double createdBefore = counterValue("operator_bookings_created_total");
        double replayBefore = counterValue("operator_idempotent_replay_total");

        operatorBookingService.createOrReuse("IDEMP-METRICS-2", "BK-METRICS-2", "TRIP-100",
                List.of(new PassengerDto("Arun Kumar")), new BigDecimal("1240.00"));

        assertThat(counterValue("operator_bookings_created_total")).isEqualTo(createdBefore);
        assertThat(counterValue("operator_idempotent_replay_total")).isEqualTo(replayBefore + 1);
    }
}
