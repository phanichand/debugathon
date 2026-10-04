package com.debugathon.problem1.payment;

import com.debugathon.problem1.payment.domain.PaymentStatus;
import com.debugathon.problem1.payment.service.PaymentService;
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
class PaymentServiceIntegrationTest {

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

    @Test
    void chargeReturnsSuccessWithGeneratedReference() {
        PaymentService.PaymentResult result = paymentService.charge("BK-000001", new BigDecimal("1240.00"));

        assertThat(result.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(result.paymentReference()).startsWith("PAY-");
    }
}
