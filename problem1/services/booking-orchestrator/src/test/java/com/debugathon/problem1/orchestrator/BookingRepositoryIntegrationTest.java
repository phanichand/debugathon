package com.debugathon.problem1.orchestrator;

import com.debugathon.problem1.orchestrator.domain.*;
import com.debugathon.problem1.orchestrator.repository.BookingAttemptRepository;
import com.debugathon.problem1.orchestrator.repository.BookingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@Transactional
class BookingRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingAttemptRepository bookingAttemptRepository;

    @Test
    void savesAndFindsBookingByReference() {
        Booking booking = new Booking("BK-000001", "CUSTOMER-21", "TRIP-100",
                new BigDecimal("1240.00"), BookingStatus.CREATED);
        bookingRepository.save(booking);

        Booking found = bookingRepository.findByBookingReference("BK-000001").orElseThrow();
        assertThat(found.getStatus()).isEqualTo(BookingStatus.CREATED);
        assertThat(found.getVersion()).isEqualTo(0L);
    }

    @Test
    void savesBookingAttemptLinkedToBooking() {
        Booking booking = new Booking("BK-000002", "CUSTOMER-22", "TRIP-100",
                new BigDecimal("500.00"), BookingStatus.CREATED);
        bookingRepository.save(booking);

        BookingAttempt attempt = new BookingAttempt(
                booking, 1, AttemptType.PAYMENT, Instant.now(), Instant.now(),
                null, "corr-1", 201, AttemptOutcome.SUCCESS, null, 42L);
        bookingAttemptRepository.save(attempt);

        assertThat(bookingAttemptRepository.findAll()).hasSize(1);
        assertThat(bookingAttemptRepository.findAll().get(0).getBooking().getBookingReference())
                .isEqualTo("BK-000002");
    }
}
