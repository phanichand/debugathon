package com.debugathon.problem1.operator.controller;

import com.debugathon.problem1.operator.dto.OperatorBookingRequest;
import com.debugathon.problem1.operator.dto.OperatorBookingResponse;
import com.debugathon.problem1.operator.repository.OperatorBookingRepository;
import com.debugathon.problem1.operator.scenario.LatencyGenerator;
import com.debugathon.problem1.operator.service.OperatorBookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/operator/bookings")
public class OperatorController {

    private final OperatorBookingService operatorBookingService;
    private final OperatorBookingRepository operatorBookingRepository;
    private final LatencyGenerator latencyGenerator;

    public OperatorController(OperatorBookingService operatorBookingService,
                               OperatorBookingRepository operatorBookingRepository,
                               LatencyGenerator latencyGenerator) {
        this.operatorBookingService = operatorBookingService;
        this.operatorBookingRepository = operatorBookingRepository;
        this.latencyGenerator = latencyGenerator;
    }

    @PostMapping
    public ResponseEntity<OperatorBookingResponse> create(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody OperatorBookingRequest request) throws InterruptedException {
        Thread.sleep(latencyGenerator.nextDelay().toMillis());
        var result = operatorBookingService.createOrReuse(
                idempotencyKey, request.sourceBookingId(), request.tripId(),
                request.passengers(), request.amount());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new OperatorBookingResponse(result.operatorBookingReference(), result.status().name()));
    }

    @GetMapping("/{operatorBookingId}")
    public ResponseEntity<OperatorBookingResponse> get(@PathVariable String operatorBookingId) {
        return operatorBookingRepository.findByOperatorBookingReference(operatorBookingId)
                .map(booking -> ResponseEntity.ok(
                        new OperatorBookingResponse(booking.getOperatorBookingReference(), booking.getStatus().name())))
                .orElse(ResponseEntity.notFound().build());
    }
}
