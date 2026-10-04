package com.debugathon.problem1.bookingapi.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CreateBookingRequest(
        @NotBlank String tripId,
        @NotBlank String customerId,
        @NotEmpty List<@Valid PassengerRequest> passengers,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount
) {
}
