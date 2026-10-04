package com.debugathon.problem1.payment;

import com.debugathon.problem1.payment.controller.PaymentController;
import com.debugathon.problem1.payment.service.PaymentService;
import com.debugathon.problem1.payment.domain.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @Test
    void chargeReturns201WithPaymentReference() throws Exception {
        when(paymentService.charge(eq("BK-000001"), any(BigDecimal.class)))
                .thenReturn(new PaymentService.PaymentResult("PAY-000001", PaymentStatus.SUCCESS));

        mockMvc.perform(post("/payments")
                        .contentType("application/json")
                        .content("{\"bookingReference\":\"BK-000001\",\"amount\":1240.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentReference").value("PAY-000001"))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }
}
