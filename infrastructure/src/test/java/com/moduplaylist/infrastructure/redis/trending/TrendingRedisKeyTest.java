package com.moduplaylist.infrastructure.redis.trending;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class TrendingRedisKeyTest {

    @Test
    void contentsBucketUsesOccurredAtTruncatedToUtcHour() {
        Instant occurredAt = Instant.parse("2026-09-22T14:59:59Z");

        assertThat(TrendingRedisKey.contentsBucket(occurredAt))
                .isEqualTo("trending:contents:2026092214");
    }

    @Test
    void activeContentBucketsReturnsCurrentAndPreviousTwentyThreeHours() {
        Instant now = Instant.parse("2026-09-23T02:30:00Z");

        List<String> buckets = TrendingRedisKey.activeContentBuckets(now, 24);

        assertThat(buckets)
                .hasSize(24)
                .first()
                .isEqualTo("trending:contents:2026092302");
        assertThat(buckets.get(23)).isEqualTo("trending:contents:2026092203");
    }

    @Test
    void eventInEarliestActiveBucketIsIncluded() {
        Instant now = Instant.parse("2026-09-23T02:30:00Z");
        Instant occurredAt = Instant.parse("2026-09-22T03:00:00Z");

        assertThat(TrendingRedisKey.isWithinActiveWindow(occurredAt, now, 24)).isTrue();
    }

    @Test
    void eventBeforeEarliestActiveBucketIsExcluded() {
        Instant now = Instant.parse("2026-09-23T02:30:00Z");
        Instant occurredAt = Instant.parse("2026-09-22T02:59:59Z");

        assertThat(TrendingRedisKey.isWithinActiveWindow(occurredAt, now, 24)).isFalse();
    }

    @Test
    void futureEventIsExcluded() {
        Instant now = Instant.parse("2026-09-23T02:30:00Z");
        Instant occurredAt = Instant.parse("2026-09-23T02:30:01Z");

        assertThat(TrendingRedisKey.isWithinActiveWindow(occurredAt, now, 24)).isFalse();
    }
}
