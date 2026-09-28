package com.moduplaylist.infrastructure.redis.oauth;

import com.moduplaylist.core.user.repository.OAuth2AuthorizationRequestStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisOAuth2AuthorizationRequestStore implements OAuth2AuthorizationRequestStore {

  private static final String KEY_PREFIX = "auth:oauth2:request:";

  private final RedisTemplate<String, Object> redisTemplate;

  @Override
  public void save(String state, String browserToken, String serializedRequest, Duration ttl) {
    redisTemplate.opsForValue().set(key(state, browserToken), serializedRequest, ttl);
  }

  @Override
  public String find(String state, String browserToken) {
    Object value = redisTemplate.opsForValue().get(key(state, browserToken));
    return value instanceof String serializedRequest ? serializedRequest : null;
  }

  @Override
  public String consume(String state, String browserToken) {
    Object value = redisTemplate.opsForValue().getAndDelete(key(state, browserToken));
    return value instanceof String serializedRequest ? serializedRequest : null;
  }

  private String key(String state, String browserToken) {
    return KEY_PREFIX + state + ":" + browserToken;
  }
}
