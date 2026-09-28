package com.moduplaylist.api.recommendation.metric;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Component
public class InitialPreferencePostProcessingMetrics {

    private static final String METRIC_PREFIX = "mopl.recommendation.postprocess";

    private final Counter successCounter;
    private final Counter failureCounter;
    private final Counter partialFailureCounter;
    private final Counter rejectedCounter;
    private final Timer durationTimer;

    public InitialPreferencePostProcessingMetrics(
            MeterRegistry meterRegistry,
            @Qualifier("recommendationPostProcessingExecutor")
            ThreadPoolTaskExecutor executor
    ) {
        successCounter = counter(meterRegistry, "success", "완료된 추천 후처리 수");
        failureCounter = counter(meterRegistry, "failure", "실패한 추천 후처리 수");
        partialFailureCounter = counter(
                meterRegistry,
                "partial.failure",
                "일부 단계 완료 후 실패한 추천 후처리 수"
        );
        rejectedCounter = counter(meterRegistry, "rejected", "executor가 거절한 후처리 수");
        durationTimer = Timer.builder(METRIC_PREFIX + ".duration")
                .description("추천 후처리 실행 시간")
                .register(meterRegistry);

        Gauge.builder(
                        METRIC_PREFIX + ".queue.size",
                        executor,
                        taskExecutor -> taskExecutor
                                .getThreadPoolExecutor()
                                .getQueue()
                                .size()
                )
                .description("추천 후처리 executor 대기 작업 수")
                .register(meterRegistry);
        Gauge.builder(
                        METRIC_PREFIX + ".active.count",
                        executor,
                        ThreadPoolTaskExecutor::getActiveCount
                )
                .description("추천 후처리 executor 실행 중 작업 수")
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

    public void recordRejected() {
        rejectedCounter.increment();
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
