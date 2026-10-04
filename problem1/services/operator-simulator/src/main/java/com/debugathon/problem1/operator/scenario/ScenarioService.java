package com.debugathon.problem1.operator.scenario;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
public class ScenarioService {

    private final AtomicReference<LatencyProfile> currentProfile = new AtomicReference<>(LatencyProfile.NORMAL);

    public LatencyProfile getCurrentProfile() {
        return currentProfile.get();
    }

    public void setCurrentProfile(LatencyProfile profile) {
        currentProfile.set(profile);
    }
}
