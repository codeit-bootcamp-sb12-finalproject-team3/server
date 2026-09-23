package com.moduplaylist.infrastructure.redis.trending;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
                    redis.call('PSETEX', KEYS[1], ARGV[1], '1')
                    redis.call('ZINCRBY', KEYS[2], ARGV[2], ARGV[3])
                    return 1
                    """, Long.class);

    private final StringRedisTemplate redisTemplate;

    public boolean applyScoreOnce(
            UUID eventId,
            UUID contentId,
            double delta,
            Duration processedEventTtl
    ) {
        if (!Double.isFinite(delta) || delta == 0.0) {
            throw new IllegalArgumentException("트렌딩 점수 변화량은 0이 아닌 유한한 값이어야 합니다.");
        }
        if (processedEventTtl == null
                || processedEventTtl.isZero()
                || processedEventTtl.isNegative()) {
            throw new IllegalArgumentException("트렌딩 이벤트 중복 방지 TTL은 0보다 커야 합니다.");
        }

        Long result = redisTemplate.execute(
                APPLY_SCORE_SCRIPT,
                List.of(
                        TrendingRedisKey.processedEvent(eventId),
                        TrendingRedisKey.CONTENTS
                ),
                Long.toString(processedEventTtl.toMillis()),
                Double.toString(delta),
                contentId.toString()
        );
        return Long.valueOf(1L).equals(result);
    }

    public List<UUID> findTopContentIds(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("트렌딩 조회 개수는 1 이상이어야 합니다.");
        }

        Set<TypedTuple<String>> values = redisTemplate.opsForZSet()
                .reverseRangeWithScores(TrendingRedisKey.CONTENTS, 0, limit - 1L);
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        return values.stream()
                .filter(tuple -> tuple.getScore() != null && tuple.getScore() > 0.0)
                .map(TypedTuple::getValue)
                .map(UUID::fromString)
                .toList();
    }

    public void remove(UUID contentId) {
        redisTemplate.opsForZSet().remove(
                TrendingRedisKey.CONTENTS,
                contentId.toString()
        );
    }
}
