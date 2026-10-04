package com.debugathon.problem1.operator.scenario;

import com.debugathon.problem1.operator.scenario.dto.ScenarioProfileRequest;
import com.debugathon.problem1.operator.scenario.dto.ScenarioStatusResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/scenarios")
public class ScenarioController {

    private final ScenarioService scenarioService;
    private final String adminToken;

    public ScenarioController(ScenarioService scenarioService, @Value("${admin.token}") String adminToken) {
        this.scenarioService = scenarioService;
        this.adminToken = adminToken;
    }

    @PostMapping("/operator")
    public ResponseEntity<ScenarioStatusResponse> setProfile(
            @RequestHeader(value = "X-Admin-Token", required = false, defaultValue = "") String providedToken,
            @Valid @RequestBody ScenarioProfileRequest request) {
        if (!adminToken.equals(providedToken)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        scenarioService.setCurrentProfile(request.profile());
        return ResponseEntity.ok(new ScenarioStatusResponse(scenarioService.getCurrentProfile().name()));
    }

    @GetMapping
    public ResponseEntity<ScenarioStatusResponse> getStatus(
            @RequestHeader(value = "X-Admin-Token", required = false, defaultValue = "") String providedToken) {
        if (!adminToken.equals(providedToken)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(new ScenarioStatusResponse(scenarioService.getCurrentProfile().name()));
    }
}
