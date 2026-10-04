package com.debugathon.problem1.operator.scenario;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ScenarioController.class)
@TestPropertySource(properties = "admin.token=test-secret-token")
class ScenarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ScenarioService scenarioService;

    @Test
    void setProfileWithoutTokenReturns403() throws Exception {
        mockMvc.perform(post("/internal/scenarios/operator")
                        .contentType("application/json")
                        .content("{\"profile\":\"DEGRADED\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void setProfileWithWrongTokenReturns403() throws Exception {
        mockMvc.perform(post("/internal/scenarios/operator")
                        .header("X-Admin-Token", "wrong-token")
                        .contentType("application/json")
                        .content("{\"profile\":\"DEGRADED\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void setProfileWithCorrectTokenUpdatesScenarioServiceAndReturns200() throws Exception {
        when(scenarioService.getCurrentProfile()).thenReturn(LatencyProfile.DEGRADED);

        mockMvc.perform(post("/internal/scenarios/operator")
                        .header("X-Admin-Token", "test-secret-token")
                        .contentType("application/json")
                        .content("{\"profile\":\"DEGRADED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile").value("DEGRADED"));

        verify(scenarioService).setCurrentProfile(LatencyProfile.DEGRADED);
    }

    @Test
    void getStatusWithoutTokenReturns403() throws Exception {
        mockMvc.perform(get("/internal/scenarios"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getStatusWithCorrectTokenReturnsCurrentProfile() throws Exception {
        when(scenarioService.getCurrentProfile()).thenReturn(LatencyProfile.NORMAL);

        mockMvc.perform(get("/internal/scenarios")
                        .header("X-Admin-Token", "test-secret-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile").value("NORMAL"));
    }
}
