package com.debugathon.problem1.operator.dto;

import jakarta.validation.constraints.NotBlank;

public record PassengerDto(@NotBlank String name) {
}
