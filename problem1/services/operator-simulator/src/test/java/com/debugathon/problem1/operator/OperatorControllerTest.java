package com.debugathon.problem1.operator;

import com.debugathon.problem1.operator.controller.OperatorController;
import com.debugathon.problem1.operator.domain.OperatorBookingStatus;
import com.debugathon.problem1.operator.repository.OperatorBookingRepository;
import com.debugathon.problem1.operator.scenario.LatencyGenerator;
import com.debugathon.problem1.operator.service.OperatorBookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OperatorController.class)
class OperatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OperatorBookingService operatorBookingService;

    @MockBean
    private OperatorBookingRepository operatorBookingRepository;

    @MockBean
    private LatencyGenerator latencyGenerator;

    @BeforeEach
    void stubZeroLatency() {
        when(latencyGenerator.nextDelay()).thenReturn(java.time.Duration.ZERO);
    }

    @Test
    void createReturns201WithOperatorBookingId() throws Exception {
        when(operatorBookingService.createOrReuse(eq("IDEMP-1"), eq("BK-000001"), eq("TRIP-100"), any(), any()))
                .thenReturn(new OperatorBookingService.OperatorBookingResult("OP-000001", OperatorBookingStatus.CONFIRMED));

        mockMvc.perform(post("/operator/bookings")
                        .header("X-Idempotency-Key", "IDEMP-1")
                        .contentType("application/json")
                        .content("{\"sourceBookingId\":\"BK-000001\",\"tripId\":\"TRIP-100\",\"passengers\":[{\"name\":\"Arun Kumar\"}],\"amount\":1240.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.operatorBookingId").value("OP-000001"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void getUnknownBookingReturns404() throws Exception {
        when(operatorBookingRepository.findByOperatorBookingReference("OP-999999"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/operator/bookings/OP-999999"))
                .andExpect(status().isNotFound());
    }
}
