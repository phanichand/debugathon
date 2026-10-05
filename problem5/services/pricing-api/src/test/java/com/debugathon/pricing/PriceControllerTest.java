package com.debugathon.pricing;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PriceController.class)
class PriceControllerTest {
    @Autowired MockMvc http;
    @MockitoBean PriceService prices;
    @Test void exposesPriceAndCorrelationHeaders() throws Exception {
        when(prices.get("A")).thenReturn(new Price("A", new BigDecimal("820.00"), "INR", 1, Instant.EPOCH));
        http.perform(get("/prices/A").header("X-Request-ID", "request-1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1))
            .andExpect(header().string("X-Request-ID", "request-1"))
            .andExpect(header().string("Cache-Control", "no-store"));
    }
}
