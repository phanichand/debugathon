package com.debugathon.problem1.bookingapi.controller;

import com.debugathon.problem1.bookingapi.client.OrchestratorClient;
import com.debugathon.problem1.bookingapi.client.OrchestratorClientException;
import com.debugathon.problem1.bookingapi.client.dto.PassengerRequest;
import com.debugathon.problem1.bookingapi.dto.BookingResponse;
import com.debugathon.problem1.bookingapi.dto.CreateBookingRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final OrchestratorClient orchestratorClient;

    public BookingController(OrchestratorClient orchestratorClient) {
        this.orchestratorClient = orchestratorClient;
    }

    @PostMapping
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody CreateBookingRequest request) {
        var orchestratorRequest = new com.debugathon.problem1.bookingapi.client.dto.CreateBookingRequest(
                request.tripId(), request.customerId(),
                request.passengers().stream().map(p -> new PassengerRequest(p.name())).toList(),
                request.amount());

        var result = orchestratorClient.create(orchestratorRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new BookingResponse(result.bookingId(), result.status(), result.operatorBookingId()));
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingResponse> get(@PathVariable String bookingId) {
        try {
            var result = orchestratorClient.get(bookingId);
            return ResponseEntity.ok(new BookingResponse(result.bookingId(), result.status(), result.operatorBookingId()));
        } catch (OrchestratorClientException clientException) {
            if (clientException.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                return ResponseEntity.notFound().build();
            }
            throw clientException;
        }
    }
}
