package com.debugathon.problem1.operator.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record OperatorBookingRequest(
        @NotBlank String sourceBookingId,
        @NotBlank String tripId,
        @NotEmpty List<@Valid PassengerDto> passengers,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount
) {
}
