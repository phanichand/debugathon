package com.debugathon.problem1.orchestrator.client;

import com.debugathon.problem1.orchestrator.client.dto.PaymentRequest;
import com.debugathon.problem1.orchestrator.client.dto.PaymentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Component
public class PaymentClient {

    private final RestClient restClient;

    public PaymentClient(RestClient.Builder builder, @Value("${payment.service.base-url}") String baseUrl) {
        // Force HTTP/1.1: the JDK HttpClient's default HTTP/2 cleartext-upgrade attempt
        // is incompatible with WireMock's server and causes spurious EOFExceptions.
        HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        this.restClient = builder.baseUrl(baseUrl)
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .build();
    }

    public PaymentResponse charge(PaymentRequest request) {
        return restClient.post()
                .uri("/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(PaymentResponse.class);
    }
}
