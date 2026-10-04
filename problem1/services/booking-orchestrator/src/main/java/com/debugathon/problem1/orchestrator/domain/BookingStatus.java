package com.debugathon.problem1.orchestrator.domain;

public enum BookingStatus {
    CREATED,
    PAYMENT_PENDING,
    PAYMENT_SUCCESS,
    OPERATOR_PENDING,
    CONFIRMED,
    FAILED,
    UNKNOWN
}
