package com.debugathon.problem1.operator.scenario;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class LatencyGeneratorTest {

    @Test
    void deterministicTimeoutProfileAlwaysReturnsTheConfiguredFixedDelay() {
        LatencyGenerator generator = generator(LatencyProfile.DETERMINISTIC_TIMEOUT);

        for (int i = 0; i < 20; i++) {
            assertThat(generator.nextDelay("BK-000001")).isEqualTo(Duration.ofMillis(3000));
        }
    }

    @Test
    void normalProfileStaysWithinDocumentedRange() {
        LatencyGenerator generator = generator(LatencyProfile.NORMAL);

        for (int i = 0; i < 50; i++) {
            Duration delay = generator.nextDelay("BK-000001");
            assertThat(delay).isGreaterThanOrEqualTo(Duration.ofMillis(200));
            assertThat(delay).isLessThan(Duration.ofMillis(600));
        }
    }

    @Test
    void recoveredProfileBehavesLikeNormal() {
        LatencyGenerator generator = generator(LatencyProfile.RECOVERED);

        for (int i = 0; i < 50; i++) {
            Duration delay = generator.nextDelay("BK-000001");
            assertThat(delay).isGreaterThanOrEqualTo(Duration.ofMillis(200));
            assertThat(delay).isLessThan(Duration.ofMillis(600));
        }
    }

    @Test
    void degradedProfileKeepsASelectedBookingSlowAcrossRetries() {
        LatencyGenerator generator = generator(LatencyProfile.DEGRADED);

        for (int i = 0; i < 20; i++) {
            Duration delay = generator.nextDelay("BK-000059");
            assertThat(delay).isGreaterThanOrEqualTo(Duration.ofMillis(2200));
            assertThat(delay).isLessThan(Duration.ofMillis(3000));
        }
    }

    @Test
    void degradedProfileKeepsANormalBookingBelowTheTimeoutBoundary() {
        LatencyGenerator generator = generator(LatencyProfile.DEGRADED);

        for (int i = 0; i < 20; i++) {
            Duration delay = generator.nextDelay("BK-000001");
            assertThat(delay).isGreaterThanOrEqualTo(Duration.ofMillis(200));
            assertThat(delay).isLessThan(Duration.ofMillis(600));
        }
    }

    @Test
    void degradedProfileTargetsRoughlyTheConfiguredShareOfSequentialBookings() {
        LatencyGenerator generator = generator(LatencyProfile.DEGRADED);
        int slow = 0;

        for (int i = 1; i <= 1000; i++) {
            Duration delay = generator.nextDelay("BK-" + String.format("%06d", i));
            if (delay.toMillis() >= 2000) {
                slow++;
            }
        }

        assertThat(slow).isBetween(10, 30);
    }

    @Test
    void severeProfileStaysWithinDocumentedOverallRange() {
        LatencyGenerator generator = generator(LatencyProfile.SEVERE);

        for (int i = 0; i < 200; i++) {
            Duration delay = generator.nextDelay("BK-000001");
            assertThat(delay).isGreaterThanOrEqualTo(Duration.ofMillis(200));
            assertThat(delay).isLessThan(Duration.ofMillis(4000));
        }
    }

    private LatencyGenerator generator(LatencyProfile profile) {
        return new LatencyGenerator(new ScenarioService(profile), 3000L, 180);
    }
}
