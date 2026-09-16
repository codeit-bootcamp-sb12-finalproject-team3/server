package com.moduplaylist.infrastructure.redis.recommendation;

import com.moduplaylist.infrastructure.recommendation.RecommendationProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ContentRecommendationRedisRepository {

    private static final String TEMP_KEY_SEPARATOR = ":tmp:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final RecommendationProperties properties;

    public void replace(UUID userId, List<UUID> orderedContentIds) {
        String key = RecommendationRedisKey.contents(userId);
        List<String> values = orderedContentIds.stream()
                .map(UUID::toString)
                .toList();
        if (values.isEmpty()) {
            redisTemplate.delete(key);
            return;
        }

        String temporaryKey = key + TEMP_KEY_SEPARATOR + UUID.randomUUID();
        try {
            Long storedCount = redisTemplate.opsForList()
                    .rightPushAll(temporaryKey, values.toArray());
            if (storedCount == null || storedCount != values.size()) {
                throw new IllegalStateException("추천 임시 캐시 저장 건수가 일치하지 않습니다.");
            }
            if (!Boolean.TRUE.equals(redisTemplate.expire(
                    temporaryKey,
                    properties.getCacheTtl()
            ))) {
                throw new IllegalStateException("추천 임시 캐시에 TTL을 설정하지 못했습니다.");
            }
            redisTemplate.rename(temporaryKey, key);
        } catch (RuntimeException exception) {
            try {
                redisTemplate.delete(temporaryKey);
            } catch (RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw exception;
        }
    }

    public RecommendationCachePage findPage(UUID userId, long offset, int limit) {
        if (offset < 0) {
            throw new IllegalArgumentException("추천 조회 offset은 음수일 수 없습니다.");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("추천 조회 limit은 1 이상이어야 합니다.");
        }

        String key = RecommendationRedisKey.contents(userId);
        List<Object> transactionResults = redisTemplate.execute(
                new SessionCallback<List<Object>>() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public <K, V> List<Object> execute(RedisOperations<K, V> operations) {
                        RedisOperations<String, Object> redisOperations =
                                (RedisOperations<String, Object>) (RedisOperations<?, ?>) operations;
                        boolean transactionStarted = false;
                        try {
                            redisOperations.multi();
                            transactionStarted = true;
                            redisOperations.opsForList().size(key);
                            redisOperations.opsForList().range(
                                    key,
                                    offset,
                                    offset + limit - 1L
                            );
                            List<Object> results = redisOperations.exec();
                            transactionStarted = false;
                            return results;
                        } catch (RuntimeException exception) {
                            if (transactionStarted) {
                                try {
                                    redisOperations.discard();
                                } catch (RuntimeException cleanupException) {
                                    exception.addSuppressed(cleanupException);
                                }
                            }
                            throw exception;
                        }
                    }
                }
        );

        if (transactionResults == null || transactionResults.size() < 2) {
            return new RecommendationCachePage(List.of(), 0L);
        }

        long totalCount = ((Number) transactionResults.get(0)).longValue();
        List<UUID> contentIds = toUuidList(transactionResults.get(1));
        return new RecommendationCachePage(contentIds, totalCount);
    }

    public void delete(UUID userId) {
        redisTemplate.delete(RecommendationRedisKey.contents(userId));
    }

    private List<UUID> toUuidList(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }

        List<UUID> contentIds = new ArrayList<>(values.size());
        for (Object item : values) {
            if (item instanceof UUID id) {
                contentIds.add(id);
            } else if (item instanceof String id) {
                contentIds.add(UUID.fromString(id));
            } else {
                throw new IllegalStateException("추천 캐시에 UUID가 아닌 값이 저장되어 있습니다.");
            }
        }
        return List.copyOf(contentIds);
    }
}
