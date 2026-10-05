package com.debugathon.problem1.operator.scenario;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
public class ScenarioService {

    private final AtomicReference<LatencyProfile> currentProfile;

    public ScenarioService(@Value("${operator.initial-profile:NORMAL}") LatencyProfile initialProfile) {
        this.currentProfile = new AtomicReference<>(initialProfile);
    }

    public LatencyProfile getCurrentProfile() {
        return currentProfile.get();
    }

    public void setCurrentProfile(LatencyProfile profile) {
        currentProfile.set(profile);
    }
}
