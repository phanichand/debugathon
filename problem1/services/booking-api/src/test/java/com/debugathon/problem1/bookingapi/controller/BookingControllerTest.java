package com.debugathon.problem1.bookingapi.controller;

import com.debugathon.problem1.bookingapi.client.OrchestratorClient;
import com.debugathon.problem1.bookingapi.client.OrchestratorClientException;
import com.debugathon.problem1.bookingapi.client.dto.BookingResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingController.class)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrchestratorClient orchestratorClient;

    @Test
    void createReturns201WithConfirmedBooking() throws Exception {
        when(orchestratorClient.create(any()))
                .thenReturn(new BookingResponse("BK-000001", "CONFIRMED", "OP-000001", new BigDecimal("1240.00"), "TRIP-100", "CUSTOMER-21"));

        mockMvc.perform(post("/api/bookings")
                        .contentType("application/json")
                        .content("{\"tripId\":\"TRIP-100\",\"customerId\":\"CUSTOMER-21\",\"passengers\":[{\"name\":\"Arun Kumar\"}],\"amount\":1240.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingId").value("BK-000001"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.operatorBookingId").value("OP-000001"));
    }

    @Test
    void createWithMissingTripIdReturns400() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType("application/json")
                        .content("{\"customerId\":\"CUSTOMER-21\",\"passengers\":[{\"name\":\"Arun Kumar\"}],\"amount\":1240.00}"))
                .andExpect(status().isBadRequest());

        verify(orchestratorClient, never()).create(any());
    }

    @Test
    void createWithNonPositiveAmountReturns400() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType("application/json")
                        .content("{\"tripId\":\"TRIP-100\",\"customerId\":\"CUSTOMER-21\",\"passengers\":[{\"name\":\"Arun Kumar\"}],\"amount\":0}"))
                .andExpect(status().isBadRequest());

        verify(orchestratorClient, never()).create(any());
    }

    @Test
    void createWithTooManyDecimalPlacesReturns400() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType("application/json")
                        .content("{\"tripId\":\"TRIP-100\",\"customerId\":\"CUSTOMER-21\",\"passengers\":[{\"name\":\"Arun Kumar\"}],\"amount\":1240.005}"))
                .andExpect(status().isBadRequest());

        verify(orchestratorClient, never()).create(any());
    }

    @Test
    void createWithEmptyPassengersReturns400() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType("application/json")
                        .content("{\"tripId\":\"TRIP-100\",\"customerId\":\"CUSTOMER-21\",\"passengers\":[],\"amount\":1240.00}"))
                .andExpect(status().isBadRequest());

        verify(orchestratorClient, never()).create(any());
    }

    @Test
    void getUnknownBookingReturns404() throws Exception {
        when(orchestratorClient.get("BK-999999"))
                .thenThrow(new OrchestratorClientException(HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/bookings/BK-999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getWithUpstreamServerErrorDoesNotMaskAs404() {
        when(orchestratorClient.get("BK-500000"))
                .thenThrow(new OrchestratorClientException(HttpStatus.INTERNAL_SERVER_ERROR));

        Exception thrown = assertThrows(Exception.class,
                () -> mockMvc.perform(get("/api/bookings/BK-500000")));

        Throwable rootCause = thrown;
        while (rootCause.getCause() != null && !(rootCause instanceof OrchestratorClientException)) {
            rootCause = rootCause.getCause();
        }
        assertEquals(OrchestratorClientException.class, rootCause.getClass());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ((OrchestratorClientException) rootCause).getStatusCode());
    }
}
