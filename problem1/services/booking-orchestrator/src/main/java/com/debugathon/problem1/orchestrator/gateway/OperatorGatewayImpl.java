package com.debugathon.problem1.orchestrator.gateway;

import com.debugathon.problem1.orchestrator.client.dto.OperatorPassengerDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class OperatorGatewayImpl implements OperatorGateway {

    private final OperatorRetryExecutor retryExecutor;

    public OperatorGatewayImpl(OperatorRetryExecutor retryExecutor) {
        this.retryExecutor = retryExecutor;
    }

    @Override
    public OperatorExecutionResult book(RequestContext context, String tripId, List<OperatorPassengerDto> passengers,
                                         BigDecimal amount) {
        return retryExecutor.execute(context, tripId, passengers, amount);
    }
}
