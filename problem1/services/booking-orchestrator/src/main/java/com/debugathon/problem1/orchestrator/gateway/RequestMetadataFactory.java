package com.debugathon.problem1.orchestrator.gateway;

import org.springframework.stereotype.Component;

@Component
public class RequestMetadataFactory {

    private final OutboundRequestIdentityFactory identityFactory;

    public RequestMetadataFactory(OutboundRequestIdentityFactory identityFactory) {
        this.identityFactory = identityFactory;
    }

    public RequestMetadata create(RequestContext context, int attemptNumber) {
        String requestIdentity = identityFactory.create(context, attemptNumber);
        return new RequestMetadata(requestIdentity, context.correlationId());
    }
}
