package com.debugathon.problem1.operator.scenario;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class LatencyGenerator {

    private final ScenarioService scenarioService;
    private final long deterministicTimeoutDelayMs;

    public LatencyGenerator(ScenarioService scenarioService,
                             @Value("${operator.deterministic-timeout-delay-ms:3000}") long deterministicTimeoutDelayMs) {
        this.scenarioService = scenarioService;
        this.deterministicTimeoutDelayMs = deterministicTimeoutDelayMs;
    }

    public Duration nextDelay() {
        LatencyProfile profile = scenarioService.getCurrentProfile();
        return switch (profile) {
            case NORMAL, RECOVERED -> Duration.ofMillis(randomBetween(200, 600));
            case DEGRADED -> Duration.ofMillis(degradedDelayMs());
            case SEVERE -> Duration.ofMillis(severeDelayMs());
            case DETERMINISTIC_TIMEOUT -> Duration.ofMillis(deterministicTimeoutDelayMs);
        };
    }

    private long degradedDelayMs() {
        double roll = ThreadLocalRandom.current().nextDouble();
        if (roll < 0.90) {
            return randomBetween(200, 600);
        } else if (roll < 0.98) {
            return randomBetween(600, 1200);
        }
        return randomBetween(2200, 3000);
    }

    private long severeDelayMs() {
        double roll = ThreadLocalRandom.current().nextDouble();
        if (roll < 0.80) {
            return randomBetween(200, 800);
        } else if (roll < 0.90) {
            return randomBetween(1000, 1800);
        }
        return randomBetween(2300, 4000);
    }

    private long randomBetween(long minInclusive, long maxExclusive) {
        return ThreadLocalRandom.current().nextLong(minInclusive, maxExclusive);
    }
}
