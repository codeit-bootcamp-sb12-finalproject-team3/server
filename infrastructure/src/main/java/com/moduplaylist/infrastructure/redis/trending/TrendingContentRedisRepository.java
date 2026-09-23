package com.moduplaylist.infrastructure.redis.trending;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import com.moduplaylist.infrastructure.trending.TrendingProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TrendingContentRedisRepository {

    private static final DefaultRedisScript<Long> APPLY_SCORE_SCRIPT =
            new DefaultRedisScript<>("""
                    if redis.call('EXISTS', KEYS[1]) == 1 then
                        return 0
                    end
                    local bucketType = redis.call('TYPE', KEYS[2]).ok
                    if bucketType ~= 'none' and bucketType ~= 'zset' then
                        return redis.error_reply('trending bucket key must be a sorted set')
                    end
                    redis.call('PSETEX', KEYS[1], ARGV[1], '1')
                    redis.call('ZINCRBY', KEYS[2], ARGV[2], ARGV[3])
                    redis.call('PEXPIREAT', KEYS[2], ARGV[4])
                    return 1
                    """, Long.class);

    private static final DefaultRedisScript<Long> AGGREGATE_BUCKETS_SCRIPT =
            new DefaultRedisScript<>("""
                    if redis.call('EXISTS', KEYS[2]) == 1 then
                        return 0
                    end
                    local sourceKeys = {}
                    for index = 3, #KEYS do
                        sourceKeys[#sourceKeys + 1] = KEYS[index]
                    end
                    redis.call('ZUNIONSTORE', KEYS[1], #sourceKeys, unpack(sourceKeys))
                    if redis.call('EXISTS', KEYS[1]) == 1 then
                        redis.call('PEXPIRE', KEYS[1], ARGV[1])
                    end
                    redis.call('PSETEX', KEYS[2], ARGV[1], '1')
                    return 1
                    """, Long.class);

    private static final DefaultRedisScript<Long> REMOVE_CONTENT_SCRIPT =
            new DefaultRedisScript<>("""
                    local removed = 0
                    for index = 1, #KEYS do
                        removed = removed + redis.call('ZREM', KEYS[index], ARGV[1])
                    end
                    return removed
                    """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final TrendingProperties properties;

    public boolean applyScoreOnce(
            UUID eventId,
            UUID contentId,
            double delta,
            Instant occurredAt,
            Instant now
    ) {
        if (!Double.isFinite(delta) || delta == 0.0) {
            throw new IllegalArgumentException("트렌딩 점수 변화량은 0이 아닌 유한한 값이어야 합니다.");
        }
        if (!TrendingRedisKey.isWithinActiveWindow(
                occurredAt,
                now,
                properties.getWindowHours()
        )) {
            return false;
        }

        Duration processedEventTtl = properties.getProcessedEventTtl();
        if (processedEventTtl == null
                || processedEventTtl.isZero()
                || processedEventTtl.isNegative()) {
            throw new IllegalArgumentException("트렌딩 이벤트 중복 방지 TTL은 0보다 커야 합니다.");
        }

        Instant bucketExpiresAt = TrendingRedisKey.bucketStart(occurredAt)
                .plus(properties.getBucketTtl().toMillis(), ChronoUnit.MILLIS);

        Long result = redisTemplate.execute(
                APPLY_SCORE_SCRIPT,
                List.of(
                        TrendingRedisKey.processedEvent(eventId),
                        TrendingRedisKey.contentsBucket(occurredAt)
                ),
                Long.toString(processedEventTtl.toMillis()),
                Double.toString(delta),
                contentId.toString(),
                Long.toString(bucketExpiresAt.toEpochMilli())
        );
        return Long.valueOf(1L).equals(result);
    }

    public List<UUID> findTopContentIds(int limit, Instant now) {
        if (limit < 1) {
            throw new IllegalArgumentException("트렌딩 조회 개수는 1 이상이어야 합니다.");
        }

        String aggregateKey = TrendingRedisKey.aggregate(now);
        aggregateBucketsIfNecessary(aggregateKey, now);

        Set<TypedTuple<String>> values = redisTemplate.opsForZSet()
                .reverseRangeWithScores(aggregateKey, 0, limit - 1L);
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        return values.stream()
                .filter(tuple -> tuple.getScore() != null && tuple.getScore() > 0.0)
                .map(TypedTuple::getValue)
                .map(UUID::fromString)
                .toList();
    }

    private void aggregateBucketsIfNecessary(String aggregateKey, Instant now) {
        List<String> keys = new ArrayList<>();
        keys.add(aggregateKey);
        keys.add(TrendingRedisKey.aggregateReady(now));
        keys.addAll(TrendingRedisKey.activeContentBuckets(now, properties.getWindowHours()));

        redisTemplate.execute(
                AGGREGATE_BUCKETS_SCRIPT,
                keys,
                Long.toString(properties.getResultCacheTtl().toMillis())
        );
    }

    public void remove(UUID contentId, Instant now) {
        List<String> keys = new ArrayList<>(
                TrendingRedisKey.activeContentBuckets(now, properties.getWindowHours())
        );
        keys.add(TrendingRedisKey.aggregate(now));
        keys.add(TrendingRedisKey.CONTENTS);

        redisTemplate.execute(
                REMOVE_CONTENT_SCRIPT,
                keys,
                contentId.toString()
        );
    }
}
