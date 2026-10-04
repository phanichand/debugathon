package com.debugathon.problem1.payment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThatCode;

@Testcontainers
class PaymentFlywayMigrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Test
    void migratesCleanlyOnFreshAndAlreadyMigratedDatabase() {
        String[] args = {
                "--spring.datasource.url=" + postgres.getJdbcUrl(),
                "--spring.datasource.username=" + postgres.getUsername(),
                "--spring.datasource.password=" + postgres.getPassword()
        };

        assertThatCode(() -> {
            ConfigurableApplicationContext first =
                    new SpringApplicationBuilder(PaymentSimulatorApplication.class).run(args);
            first.close();

            ConfigurableApplicationContext second =
                    new SpringApplicationBuilder(PaymentSimulatorApplication.class).run(args);
            second.close();
        }).doesNotThrowAnyException();
    }
}
