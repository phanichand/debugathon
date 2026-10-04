package com.debugathon.problem1.operator;

import com.debugathon.problem1.operator.dto.PassengerDto;
import com.debugathon.problem1.operator.repository.OperatorBookingRepository;
import com.debugathon.problem1.operator.service.OperatorBookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class OperatorBookingServiceIntegrationTest {

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
    private OperatorBookingRepository operatorBookingRepository;

    @Test
    void sameKeyReturnsSameBooking() {
        var first = operatorBookingService.createOrReuse(
                "IDEMP-1", "BK-000001", "TRIP-100", List.of(new PassengerDto("Arun Kumar")), new BigDecimal("1240.00"));
        var second = operatorBookingService.createOrReuse(
                "IDEMP-1", "BK-000001", "TRIP-100", List.of(new PassengerDto("Arun Kumar")), new BigDecimal("1240.00"));

        assertThat(second.operatorBookingReference()).isEqualTo(first.operatorBookingReference());
        assertThat(operatorBookingRepository.count()).isEqualTo(1);
    }

    @Test
    void differentKeysCreateDifferentBookings() {
        var first = operatorBookingService.createOrReuse(
                "IDEMP-A", "BK-000002", "TRIP-100", List.of(new PassengerDto("Arun Kumar")), new BigDecimal("1240.00"));
        var second = operatorBookingService.createOrReuse(
                "IDEMP-B", "BK-000002", "TRIP-100", List.of(new PassengerDto("Arun Kumar")), new BigDecimal("1240.00"));

        assertThat(second.operatorBookingReference()).isNotEqualTo(first.operatorBookingReference());
    }

    @Test
    void concurrentRequestsWithSameKeyProduceExactlyOneBooking() throws InterruptedException, ExecutionException {
        int threadCount = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);

        List<Future<OperatorBookingService.OperatorBookingResult>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    return operatorBookingService.createOrReuse(
                            "IDEMP-RACE", "BK-000003", "TRIP-100",
                            List.of(new PassengerDto("Arun Kumar")), new BigDecimal("1240.00"));
                } finally {
                    done.countDown();
                }
            }));
        }

        ready.await();
        start.countDown();
        done.await();
        executor.shutdown();

        // Every thread must get back a valid result, not an exception -- this is the property
        // the test exists to prove. Calling .get() on each Future surfaces any exception a
        // racing call threw instead of letting it be silently swallowed.
        List<OperatorBookingService.OperatorBookingResult> results = new ArrayList<>();
        for (Future<OperatorBookingService.OperatorBookingResult> future : futures) {
            results.add(future.get());
        }

        assertThat(results).hasSize(threadCount);
        String winningReference = results.get(0).operatorBookingReference();
        assertThat(results).allSatisfy(result ->
                assertThat(result.operatorBookingReference()).isEqualTo(winningReference));

        long countForKey = operatorBookingRepository.findAll().stream()
                .filter(booking -> "IDEMP-RACE".equals(booking.getIdempotencyKey()))
                .count();
        assertThat(countForKey).isEqualTo(1);
    }
}
