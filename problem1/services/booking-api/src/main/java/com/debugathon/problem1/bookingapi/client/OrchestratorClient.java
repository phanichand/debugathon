package com.debugathon.problem1.bookingapi.client;

import com.debugathon.problem1.bookingapi.client.dto.BookingResponse;
import com.debugathon.problem1.bookingapi.client.dto.CreateBookingRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OrchestratorClient {

    private final RestClient restClient;

    public OrchestratorClient(RestClient.Builder builder, @Value("${orchestrator.service.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public BookingResponse create(CreateBookingRequest request) {
        return restClient.post()
                .uri("/internal/orchestrator/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(BookingResponse.class);
    }

    public BookingResponse get(String bookingId) {
        try {
            return restClient.get()
                    .uri("/internal/orchestrator/bookings/{bookingId}", bookingId)
                    .retrieve()
                    .body(BookingResponse.class);
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound notFound) {
            throw new OrchestratorClientException(HttpStatusCode.valueOf(404));
        }
    }
}
