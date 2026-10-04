package com.debugathon.problem1.payment.controller;

import com.debugathon.problem1.payment.dto.PaymentRequest;
import com.debugathon.problem1.payment.dto.PaymentResponse;
import com.debugathon.problem1.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/payments")
    public ResponseEntity<PaymentResponse> charge(@Valid @RequestBody PaymentRequest request) {
        PaymentService.PaymentResult result = paymentService.charge(request.bookingReference(), request.amount());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new PaymentResponse(result.paymentReference(), result.status().name()));
    }
}
