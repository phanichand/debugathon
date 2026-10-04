package com.debugathon.problem1.orchestrator.client;

import com.debugathon.problem1.orchestrator.client.dto.OperatorBookingRequest;
import com.debugathon.problem1.orchestrator.client.dto.OperatorBookingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Component
public class OperatorClient {

    private final RestClient restClient;

    public OperatorClient(RestClient.Builder builder, @Value("${operator.service.base-url}") String baseUrl,
                           @Value("${operator.timeout-ms:2000}") long timeoutMs) {
        // Force HTTP/1.1: the JDK HttpClient's default HTTP/2 cleartext-upgrade attempt
        // is incompatible with WireMock's server and causes spurious EOFExceptions.
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(timeoutMs));
        this.restClient = builder.baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public OperatorBookingResponse book(String idempotencyKey, String correlationId, OperatorBookingRequest request) {
        return restClient.post()
                .uri("/operator/bookings")
                .header("X-Idempotency-Key", idempotencyKey)
                .header("X-Correlation-Id", correlationId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(OperatorBookingResponse.class);
    }
}
