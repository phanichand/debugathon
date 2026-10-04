package com.debugathon.problem1.orchestrator.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentResponse(String paymentReference, String status) {
}
