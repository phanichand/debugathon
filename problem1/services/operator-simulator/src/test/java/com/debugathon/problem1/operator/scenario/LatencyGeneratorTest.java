package com.debugathon.problem1.operator.scenario;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class LatencyGeneratorTest {

    private final ScenarioService scenarioService = new ScenarioService();
    private final LatencyGenerator latencyGenerator = new LatencyGenerator(scenarioService, 3000L);

    @Test
    void deterministicTimeoutProfileAlwaysReturnsTheConfiguredFixedDelay() {
        scenarioService.setCurrentProfile(LatencyProfile.DETERMINISTIC_TIMEOUT);

        for (int i = 0; i < 20; i++) {
            assertThat(latencyGenerator.nextDelay()).isEqualTo(Duration.ofMillis(3000));
        }
    }

    @Test
    void normalProfileStaysWithinDocumentedRange() {
        scenarioService.setCurrentProfile(LatencyProfile.NORMAL);

        for (int i = 0; i < 50; i++) {
            Duration delay = latencyGenerator.nextDelay();
            assertThat(delay).isGreaterThanOrEqualTo(Duration.ofMillis(200));
            assertThat(delay).isLessThan(Duration.ofMillis(600));
        }
    }

    @Test
    void recoveredProfileBehavesLikeNormal() {
        scenarioService.setCurrentProfile(LatencyProfile.RECOVERED);

        for (int i = 0; i < 50; i++) {
            Duration delay = latencyGenerator.nextDelay();
            assertThat(delay).isGreaterThanOrEqualTo(Duration.ofMillis(200));
            assertThat(delay).isLessThan(Duration.ofMillis(600));
        }
    }

    @Test
    void degradedProfileStaysWithinDocumentedOverallRange() {
        scenarioService.setCurrentProfile(LatencyProfile.DEGRADED);

        for (int i = 0; i < 200; i++) {
            Duration delay = latencyGenerator.nextDelay();
            assertThat(delay).isGreaterThanOrEqualTo(Duration.ofMillis(200));
            assertThat(delay).isLessThan(Duration.ofMillis(3000));
        }
    }

    @Test
    void severeProfileStaysWithinDocumentedOverallRange() {
        scenarioService.setCurrentProfile(LatencyProfile.SEVERE);

        for (int i = 0; i < 200; i++) {
            Duration delay = latencyGenerator.nextDelay();
            assertThat(delay).isGreaterThanOrEqualTo(Duration.ofMillis(200));
            assertThat(delay).isLessThan(Duration.ofMillis(4000));
        }
    }
}
