package com.debugathon.problem1.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PaymentRequest(
        @NotBlank String bookingReference,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount
) {
}
