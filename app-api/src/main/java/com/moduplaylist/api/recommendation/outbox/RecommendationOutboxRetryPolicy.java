package com.moduplaylist.api.recommendation.outbox;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class RecommendationOutboxRetryPolicy {

    private static final List<Duration> RETRY_DELAYS = List.of(
            Duration.ofSeconds(10),
            Duration.ofSeconds(30),
            Duration.ofSeconds(60)
    );

    public Optional<Instant> nextRetryAt(int retryCount, Instant failedAt) {
        if (retryCount < 0) {
            throw new IllegalArgumentException("retryCount must not be negative");
        }
        Objects.requireNonNull(failedAt);
        if (retryCount >= RETRY_DELAYS.size()) {
            return Optional.empty();
        }
        return Optional.of(failedAt.plus(RETRY_DELAYS.get(retryCount)));
    }
}
