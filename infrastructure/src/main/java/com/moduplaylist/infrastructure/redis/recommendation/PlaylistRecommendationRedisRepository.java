package com.moduplaylist.infrastructure.redis.recommendation;

import com.moduplaylist.infrastructure.recommendation.RecommendationProperties;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PlaylistRecommendationRedisRepository {

    private static final String TEMP_KEY_SEPARATOR = ":tmp:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final RecommendationProperties properties;

    public void replace(UUID userId, List<UUID> orderedPlaylistIds) {
        String key = RecommendationRedisKey.playlists(userId);
        List<String> values = orderedPlaylistIds.stream()
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
                throw new IllegalStateException("플레이리스트 추천 임시 캐시 저장 건수가 일치하지 않습니다.");
            }
            if (!Boolean.TRUE.equals(redisTemplate.expire(
                    temporaryKey,
                    properties.getCacheTtl()
            ))) {
                throw new IllegalStateException("플레이리스트 추천 임시 캐시에 TTL을 설정하지 못했습니다.");
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
}
