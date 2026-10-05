package com.debugathon.problem1.orchestrator.gateway;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class OutboundRequestIdentityFactory {

    public String create(RequestContext context, int attemptNumber) {
        String material = context.correlationId() + ":" + attemptNumber;
        return UUID.nameUUIDFromBytes(material.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
