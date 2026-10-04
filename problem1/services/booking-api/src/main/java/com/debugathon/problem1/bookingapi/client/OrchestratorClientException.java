package com.debugathon.problem1.bookingapi.client;

import org.springframework.http.HttpStatusCode;

public class OrchestratorClientException extends RuntimeException {

    private final HttpStatusCode statusCode;

    public OrchestratorClientException(HttpStatusCode statusCode) {
        super("Orchestrator call failed with status " + statusCode);
        this.statusCode = statusCode;
    }

    public HttpStatusCode getStatusCode() {
        return statusCode;
    }
}
