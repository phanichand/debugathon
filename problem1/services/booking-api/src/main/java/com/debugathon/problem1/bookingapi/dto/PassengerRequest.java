package com.debugathon.problem1.bookingapi.dto;

import jakarta.validation.constraints.NotBlank;

public record PassengerRequest(@NotBlank String name) {
}
