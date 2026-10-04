package com.debugathon.problem1.orchestrator.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OperatorBookingResponse(String operatorBookingId, String status) {
}
