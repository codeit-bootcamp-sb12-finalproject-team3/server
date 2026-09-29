package com.moduplaylist.infrastructure.redis.auth;

import com.moduplaylist.core.user.repository.TemporaryPasswordStore;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisTemporaryPasswordStore implements TemporaryPasswordStore {

  private static final String KEY_PREFIX = "auth:password-reset:";

  private final RedisTemplate<String, Object> redisTemplate;

  @Override
  public void save(UUID userId, String encodedPassword, Duration ttl) {
    redisTemplate.opsForValue().set(key(userId), encodedPassword, ttl);
  }

  @Override
  public String findByUserId(UUID userId) {
    Object value = redisTemplate.opsForValue().get(key(userId));
    return value instanceof String encodedPassword ? encodedPassword : null;
  }

  @Override
  public void delete(UUID userId) {
    redisTemplate.delete(key(userId));
  }

  private String key(UUID userId) {
    return KEY_PREFIX + userId;
  }
}
