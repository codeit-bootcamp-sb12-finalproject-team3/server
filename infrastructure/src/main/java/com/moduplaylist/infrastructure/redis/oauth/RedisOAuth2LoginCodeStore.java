package com.moduplaylist.infrastructure.redis.oauth;

import com.moduplaylist.core.user.repository.OAuth2LoginCodeStore;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisOAuth2LoginCodeStore implements OAuth2LoginCodeStore {

  private static final String KEY_PREFIX = "auth:oauth2:login-code:";

  private final RedisTemplate<String, Object> redisTemplate;

  @Override
  public void save(String code, String browserToken, String userId, Duration ttl) {
    redisTemplate.opsForValue().set(key(code, browserToken), userId, ttl);
  }

  @Override
  public String consume(String code, String browserToken) {
    Object value = redisTemplate.opsForValue().getAndDelete(key(code, browserToken));
    return value instanceof String userId ? userId : null;
  }

  private String key(String code, String browserToken) {
    return KEY_PREFIX + code + ":" + browserToken;
  }
}
