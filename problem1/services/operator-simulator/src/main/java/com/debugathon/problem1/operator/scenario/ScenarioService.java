package com.debugathon.problem1.operator.scenario;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ScenarioService {

    private final LatencyProfile currentProfile;

    public ScenarioService(@Value("${operator.initial-profile:NORMAL}") LatencyProfile initialProfile) {
        this.currentProfile = initialProfile;
    }

    public LatencyProfile getCurrentProfile() {
        return currentProfile;
    }
}
