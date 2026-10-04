package com.debugathon.problem1.operator.scenario.dto;

import com.debugathon.problem1.operator.scenario.LatencyProfile;
import jakarta.validation.constraints.NotNull;

public record ScenarioProfileRequest(@NotNull LatencyProfile profile) {
}
