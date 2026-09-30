package com.moduplaylist.infrastructure.redis.recommendation;

import com.moduplaylist.infrastructure.recommendation.RecommendationProperties;
import java.util.ArrayList;
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

    public List<UUID> findAll(UUID userId) {
        List<Object> values = redisTemplate.opsForList()
                .range(RecommendationRedisKey.playlists(userId), 0, -1);
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        List<UUID> playlistIds = new ArrayList<>(values.size());
        for (Object value : values) {
            if (value instanceof UUID id) {
                playlistIds.add(id);
            } else if (value instanceof String id) {
                playlistIds.add(UUID.fromString(id));
            } else {
                throw new IllegalStateException("플레이리스트 추천 캐시에 UUID가 아닌 값이 저장되어 있습니다.");
            }
        }
        return List.copyOf(playlistIds);
    }

    public void delete(UUID userId) {
        redisTemplate.delete(RecommendationRedisKey.playlists(userId));
    }
}
