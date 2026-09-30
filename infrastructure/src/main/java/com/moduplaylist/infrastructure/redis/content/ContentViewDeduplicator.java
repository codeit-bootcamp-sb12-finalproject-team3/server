package com.moduplaylist.infrastructure.redis.content;

import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ContentViewDeduplicator {

	private static final String KEY_PREFIX = "view:";
	private static final Duration VIEW_TTL = Duration.ofMinutes(30);

	private final RedisTemplate<String, Object> redisTemplate;

	public boolean registerFirstView(UUID userId, UUID contentId) {
		String key = KEY_PREFIX + userId + ":" + contentId;
		return Boolean.TRUE.equals(
			redisTemplate.opsForValue().setIfAbsent(key, "1", VIEW_TTL)
		);
	}
}
