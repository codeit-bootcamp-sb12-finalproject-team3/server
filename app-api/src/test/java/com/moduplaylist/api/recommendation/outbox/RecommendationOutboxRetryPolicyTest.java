package com.moduplaylist.api.recommendation.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class RecommendationOutboxRetryPolicyTest {

    private static final Instant FAILED_AT = Instant.parse("2026-09-29T14:00:00Z");

    private final RecommendationOutboxRetryPolicy retryPolicy =
            new RecommendationOutboxRetryPolicy();

    @Test
    void schedulesTenThirtyAndSixtySecondRetries() {
        assertThat(retryPolicy.nextRetryAt(0, FAILED_AT))
                .contains(FAILED_AT.plusSeconds(10));
        assertThat(retryPolicy.nextRetryAt(1, FAILED_AT))
                .contains(FAILED_AT.plusSeconds(30));
        assertThat(retryPolicy.nextRetryAt(2, FAILED_AT))
                .contains(FAILED_AT.plusSeconds(60));
    }

    @Test
    void returnsEmptyAfterThreeRetries() {
        assertThat(retryPolicy.nextRetryAt(3, FAILED_AT)).isEmpty();
    }

    @Test
    void rejectsNegativeRetryCount() {
        assertThatThrownBy(() -> retryPolicy.nextRetryAt(-1, FAILED_AT))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
