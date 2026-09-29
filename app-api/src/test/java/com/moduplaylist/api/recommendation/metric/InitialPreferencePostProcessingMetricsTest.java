package com.moduplaylist.api.recommendation.metric;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class InitialPreferencePostProcessingMetricsTest {

    @Test
    void recordsOutcomesAndDuration() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

        try {
            InitialPreferencePostProcessingMetrics metrics =
                    new InitialPreferencePostProcessingMetrics(meterRegistry);

            metrics.recordSuccess();
            metrics.recordFailure(true);
            metrics.recordDuration(Duration.ofMillis(250));

            assertThat(counter(meterRegistry, "success")).isEqualTo(1.0);
            assertThat(counter(meterRegistry, "failure")).isEqualTo(1.0);
            assertThat(counter(meterRegistry, "partial.failure")).isEqualTo(1.0);
            assertThat(meterRegistry.get(metric("duration")).timer().count()).isEqualTo(1);
            assertThat(meterRegistry.get(metric("duration"))
                    .timer()
                    .totalTime(TimeUnit.MILLISECONDS)).isEqualTo(250.0);
        } finally {
            meterRegistry.close();
        }
    }

    private double counter(SimpleMeterRegistry meterRegistry, String suffix) {
        return meterRegistry.get(metric(suffix)).counter().count();
    }

    private String metric(String suffix) {
        return "mopl.recommendation.postprocess." + suffix;
    }
}
