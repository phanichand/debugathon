package com.debugathon.problem1.orchestrator.gateway;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RequestMetadataFactory {

    public RequestMetadata create(RequestContext context, int attemptNumber) {
        String idempotencyKey = UUID.randomUUID().toString();
        return new RequestMetadata(idempotencyKey, context.correlationId());
    }
}
