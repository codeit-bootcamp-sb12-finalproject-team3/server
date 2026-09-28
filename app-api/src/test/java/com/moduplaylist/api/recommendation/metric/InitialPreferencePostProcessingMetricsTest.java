package com.moduplaylist.api.recommendation.metric;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class InitialPreferencePostProcessingMetricsTest {

    @Test
    void recordsOutcomesDurationAndExecutorState() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ThreadPoolTaskExecutor executor = executor();

        try {
            InitialPreferencePostProcessingMetrics metrics =
                    new InitialPreferencePostProcessingMetrics(meterRegistry, executor);

            metrics.recordSuccess();
            metrics.recordFailure(true);
            metrics.recordRejected();
            metrics.recordDuration(Duration.ofMillis(250));

            assertThat(counter(meterRegistry, "success")).isEqualTo(1.0);
            assertThat(counter(meterRegistry, "failure")).isEqualTo(1.0);
            assertThat(counter(meterRegistry, "partial.failure")).isEqualTo(1.0);
            assertThat(counter(meterRegistry, "rejected")).isEqualTo(1.0);
            assertThat(meterRegistry.get(metric("duration")).timer().count()).isEqualTo(1);
            assertThat(meterRegistry.get(metric("duration"))
                    .timer()
                    .totalTime(TimeUnit.MILLISECONDS)).isEqualTo(250.0);
            assertThat(meterRegistry.get(metric("queue.size")).gauge().value()).isZero();
            assertThat(meterRegistry.get(metric("active.count")).gauge().value()).isZero();
        } finally {
            executor.shutdown();
            meterRegistry.close();
        }
    }

    private ThreadPoolTaskExecutor executor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1);
        executor.initialize();
        return executor;
    }

    private double counter(SimpleMeterRegistry meterRegistry, String suffix) {
        return meterRegistry.get(metric(suffix)).counter().count();
    }

    private String metric(String suffix) {
        return "mopl.recommendation.postprocess." + suffix;
    }
}
