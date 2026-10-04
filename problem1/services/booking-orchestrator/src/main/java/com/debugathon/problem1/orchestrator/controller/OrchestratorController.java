package com.debugathon.problem1.orchestrator.controller;

import com.debugathon.problem1.orchestrator.controller.dto.BookingResponse;
import com.debugathon.problem1.orchestrator.controller.dto.CreateBookingRequest;
import com.debugathon.problem1.orchestrator.service.BookingOrchestrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/orchestrator/bookings")
public class OrchestratorController {

    private final BookingOrchestrationService orchestrationService;

    public OrchestratorController(BookingOrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    @PostMapping
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody CreateBookingRequest request) {
        var result = orchestrationService.createBooking(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(BookingResponse.from(result));
    }

    @GetMapping("/{bookingReference}")
    public ResponseEntity<BookingResponse> get(@PathVariable String bookingReference) {
        return orchestrationService.findByReference(bookingReference)
                .map(result -> ResponseEntity.ok(BookingResponse.from(result)))
                .orElse(ResponseEntity.notFound().build());
    }
}
