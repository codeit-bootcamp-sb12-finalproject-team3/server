package com.moduplaylist.api.recommendation.metric;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class InitialPreferencePostProcessingMetrics {

    private static final String METRIC_PREFIX = "mopl.recommendation.postprocess";

    private final Counter successCounter;
    private final Counter failureCounter;
    private final Counter partialFailureCounter;
    private final Timer durationTimer;

    public InitialPreferencePostProcessingMetrics(MeterRegistry meterRegistry) {
        successCounter = counter(meterRegistry, "success", "완료된 추천 후처리 수");
        failureCounter = counter(meterRegistry, "failure", "실패한 추천 후처리 수");
        partialFailureCounter = counter(
                meterRegistry,
                "partial.failure",
                "일부 단계 완료 후 실패한 추천 후처리 수"
        );
        durationTimer = Timer.builder(METRIC_PREFIX + ".duration")
                .description("추천 후처리 실행 시간")
                .register(meterRegistry);
    }

    public void recordSuccess() {
        successCounter.increment();
    }

    public void recordFailure(boolean partialFailure) {
        failureCounter.increment();
        if (partialFailure) {
            partialFailureCounter.increment();
        }
    }

    public void recordDuration(Duration duration) {
        durationTimer.record(duration);
    }

    private Counter counter(MeterRegistry meterRegistry, String suffix, String description) {
        return Counter.builder(METRIC_PREFIX + "." + suffix)
                .description(description)
                .register(meterRegistry);
    }
}
