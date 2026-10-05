package com.debugathon.problem1.operator.scenario;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class LatencyGenerator {

    private static final int COHORT_BUCKETS = 10_000;

    private final ScenarioService scenarioService;
    private final long deterministicTimeoutDelayMs;
    private final int degradedSlowBookingBasisPoints;

    public LatencyGenerator(ScenarioService scenarioService,
                            @Value("${operator.deterministic-timeout-delay-ms:3000}") long deterministicTimeoutDelayMs,
                            @Value("${operator.degraded.slow-booking-basis-points:180}") int degradedSlowBookingBasisPoints) {
        this.scenarioService = scenarioService;
        this.deterministicTimeoutDelayMs = deterministicTimeoutDelayMs;
        this.degradedSlowBookingBasisPoints = degradedSlowBookingBasisPoints;
    }

    public Duration nextDelay(String sourceBookingId) {
        LatencyProfile profile = scenarioService.getCurrentProfile();
        return switch (profile) {
            case NORMAL, RECOVERED -> Duration.ofMillis(randomBetween(200, 600));
            case DEGRADED -> Duration.ofMillis(degradedDelayMs(sourceBookingId));
            case SEVERE -> Duration.ofMillis(severeDelayMs());
            case DETERMINISTIC_TIMEOUT -> Duration.ofMillis(deterministicTimeoutDelayMs);
        };
    }

    private long degradedDelayMs(String sourceBookingId) {
        if (belongsToSlowCohort(sourceBookingId)) {
            return randomBetween(2200, 3000);
        }
        return randomBetween(200, 600);
    }

    private boolean belongsToSlowCohort(String sourceBookingId) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(sourceBookingId.getBytes(StandardCharsets.UTF_8));
            long bucket = Integer.toUnsignedLong(ByteBuffer.wrap(digest).getInt()) % COHORT_BUCKETS;
            return bucket < degradedSlowBookingBasisPoints;
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is not available", impossible);
        }
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
