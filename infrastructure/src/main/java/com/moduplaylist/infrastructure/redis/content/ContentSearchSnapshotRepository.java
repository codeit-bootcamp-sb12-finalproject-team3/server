package com.moduplaylist.infrastructure.redis.content;

import com.moduplaylist.core.content.exception.ContentSearchUnavailableException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ContentSearchSnapshotRepository {

	private static final String KEY_PREFIX = "content:search:snapshot:";
	private static final Duration SNAPSHOT_TTL = Duration.ofMinutes(10);

	private final RedisTemplate<String, Object> redisTemplate;

	public UUID create(String signature, List<UUID> contentIds) {
		UUID snapshotId = UUID.randomUUID();
		String key = key(snapshotId);
		List<String> values = new ArrayList<>(contentIds.size() + 1);
		values.add(signature);
		contentIds.stream().map(UUID::toString).forEach(values::add);
		try {
			Long storedCount = redisTemplate.opsForList().rightPushAll(key, values.toArray());
			if (storedCount == null || storedCount != values.size()
				|| !Boolean.TRUE.equals(redisTemplate.expire(key, SNAPSHOT_TTL))) {
				throw new ContentSearchUnavailableException();
			}
			return snapshotId;
		} catch (ContentSearchUnavailableException exception) {
			try {
				redisTemplate.delete(key);
			} catch (RuntimeException cleanupException) {
				exception.addSuppressed(cleanupException);
			}
			throw exception;
		} catch (RuntimeException exception) {
			try {
				redisTemplate.delete(key);
			} catch (RuntimeException cleanupException) {
				exception.addSuppressed(cleanupException);
			}
			throw new ContentSearchUnavailableException(exception);
		}
	}

	public Optional<List<UUID>> find(UUID snapshotId, String signature) {
		String key = key(snapshotId);
		try {
			List<Object> values = redisTemplate.opsForList().range(key, 0, -1);
			if (values == null || values.isEmpty() || !signature.equals(values.get(0))) {
				return Optional.empty();
			}
			redisTemplate.expire(key, SNAPSHOT_TTL);
			List<UUID> contentIds = values.subList(1, values.size()).stream()
				.map(String::valueOf)
				.map(UUID::fromString)
				.toList();
			return Optional.of(contentIds);
		} catch (IllegalArgumentException exception) {
			return Optional.empty();
		} catch (RuntimeException exception) {
			throw new ContentSearchUnavailableException(exception);
		}
	}

	private String key(UUID snapshotId) {
		return KEY_PREFIX + snapshotId;
	}
}
