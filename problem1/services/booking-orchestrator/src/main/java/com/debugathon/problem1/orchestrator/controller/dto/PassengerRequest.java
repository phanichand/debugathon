package com.debugathon.problem1.orchestrator.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record PassengerRequest(@NotBlank String name) {
}
