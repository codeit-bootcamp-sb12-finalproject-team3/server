package com.moduplaylist.infrastructure.redis.trending;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

public final class TrendingRedisKey {

    // 시간 버킷 도입 이전의 누적 점수 키. 마이그레이션 정리에만 사용한다.
    public static final String CONTENTS = "trending:contents";

    private static final String CONTENTS_PREFIX = "trending:contents:";
    private static final String AGGREGATE_PREFIX = CONTENTS_PREFIX + "aggregate:";
    private static final String PROCESSED_EVENT_PREFIX = "trending:processed-event:";
    private static final DateTimeFormatter BUCKET_FORMATTER = DateTimeFormatter
            .ofPattern("yyyyMMddHH")
            .withZone(ZoneOffset.UTC);

    private TrendingRedisKey() {
    }

    public static String processedEvent(UUID eventId) {
        return PROCESSED_EVENT_PREFIX + eventId;
    }

    public static String contentsBucket(Instant occurredAt) {
        return CONTENTS_PREFIX + BUCKET_FORMATTER.format(bucketStart(occurredAt));
    }

    public static List<String> activeContentBuckets(Instant now, int windowHours) {
        if (windowHours < 1) {
            throw new IllegalArgumentException("트렌딩 집계 시간은 1시간 이상이어야 합니다.");
        }

        Instant currentBucket = bucketStart(now);
        return IntStream.range(0, windowHours)
                .mapToObj(offset -> contentsBucket(currentBucket.minus(offset, ChronoUnit.HOURS)))
                .toList();
    }

    public static String aggregate(Instant now) {
        return AGGREGATE_PREFIX + BUCKET_FORMATTER.format(bucketStart(now));
    }

    public static String aggregateReady(Instant now) {
        return aggregate(now) + ":ready";
    }

    public static Instant bucketStart(Instant instant) {
        if (instant == null) {
            throw new IllegalArgumentException("트렌딩 버킷 기준 시각은 필수입니다.");
        }
        return instant.truncatedTo(ChronoUnit.HOURS);
    }

    public static boolean isWithinActiveWindow(
            Instant occurredAt,
            Instant now,
            int windowHours
    ) {
        if (occurredAt == null || now == null) {
            return false;
        }
        if (windowHours < 1) {
            throw new IllegalArgumentException("트렌딩 집계 시간은 1시간 이상이어야 합니다.");
        }
        if (occurredAt.isAfter(now)) {
            return false;
        }

        Instant eventBucket = bucketStart(occurredAt);
        Instant currentBucket = bucketStart(now);
        Instant earliestBucket = currentBucket.minus(windowHours - 1L, ChronoUnit.HOURS);
        return !eventBucket.isBefore(earliestBucket) && !eventBucket.isAfter(currentBucket);
    }
}
