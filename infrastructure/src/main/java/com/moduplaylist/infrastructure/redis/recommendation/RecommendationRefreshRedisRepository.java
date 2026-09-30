package com.moduplaylist.infrastructure.redis.recommendation;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RecommendationRefreshRedisRepository {

    private static final DefaultRedisScript<String> CLAIM_SCRIPT = new DefaultRedisScript<>(
            """
            local accumulated = redis.call('GET', KEYS[1])
            if not accumulated then
                return '0'
            end
            redis.call('SET', KEYS[1], '0')
            return accumulated
            """,
            String.class
    );

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            """
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('DEL', KEYS[1])
            end
            return 0
            """,
            Long.class
    );

    private final StringRedisTemplate redisTemplate;

    public double addActivity(UUID userId, double amount) {
        validateAmount(amount);
        Double accumulated = redisTemplate.opsForValue().increment(
                RecommendationRedisKey.refreshActivity(userId),
                amount
        );
        if (accumulated == null) {
            throw new IllegalStateException("추천 갱신 활동량을 누적하지 못했습니다.");
        }
        return accumulated;
    }

    public double getActivity(UUID userId) {
        String value = redisTemplate.opsForValue().get(
                RecommendationRedisKey.refreshActivity(userId)
        );
        return value == null ? 0.0 : Double.parseDouble(value);
    }

    public boolean tryLock(UUID userId, String token, Duration ttl) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(
                RecommendationRedisKey.refreshLock(userId),
                token,
                ttl
        ));
    }

    public double claimActivity(UUID userId) {
        String claimed = redisTemplate.execute(
                CLAIM_SCRIPT,
                List.of(RecommendationRedisKey.refreshActivity(userId))
        );
        return claimed == null ? 0.0 : Double.parseDouble(claimed);
    }

    public void restoreActivity(UUID userId, double claimedAmount) {
        if (claimedAmount > 0.0) {
            addActivity(userId, claimedAmount);
        }
    }

    public boolean unlock(UUID userId, String token) {
        Long result = redisTemplate.execute(
                UNLOCK_SCRIPT,
                List.of(RecommendationRedisKey.refreshLock(userId)),
                token
        );
        return result != null && result == 1L;
    }

    private void validateAmount(double amount) {
        if (!Double.isFinite(amount) || amount <= 0.0) {
            throw new IllegalArgumentException("추천 갱신 활동량은 0보다 큰 유한한 값이어야 합니다.");
        }
    }
}
