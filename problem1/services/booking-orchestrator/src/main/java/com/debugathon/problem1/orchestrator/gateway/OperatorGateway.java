package com.debugathon.problem1.orchestrator.gateway;

import com.debugathon.problem1.orchestrator.client.dto.OperatorPassengerDto;

import java.math.BigDecimal;
import java.util.List;

public interface OperatorGateway {
    OperatorExecutionResult book(RequestContext context, String tripId, List<OperatorPassengerDto> passengers, BigDecimal amount);
}
