package com.moduplaylist.infrastructure.redis.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moduplaylist.core.user.repository.JwtRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import software.amazon.awssdk.annotations.NotNull;

@Component
@RequiredArgsConstructor
public class RedisJwtRegistry implements JwtRegistry {

  private static final String KEY_PREFIX = "auth:jwt:";

  private final RedisTemplate<String, Object> redisTemplate;
  private final ObjectMapper objectMapper;


  @Override
  public void register(
      UUID userId,
      String accessTokenId,
      String refreshTokenId,
      Instant refreshExpiresAt
  ) {
    Assert.hasText(accessTokenId, "Access Token ID가 필요합니다.");
    Assert.hasText(refreshTokenId, "Refresh Token ID가 필요합니다.");

    JwtInformation information =
        new JwtInformation(accessTokenId, refreshTokenId);

    redisTemplate.opsForValue().set(
        KEY_PREFIX + userId,
        information,
        remainingTtl(refreshExpiresAt)
    );
  }

  @Override
  public boolean isAccessTokenActive(UUID userId, String accessTokenId) {
    if (accessTokenId == null || accessTokenId.isBlank()) {
      return false;
    }

    Object value = redisTemplate.opsForValue().get(KEY_PREFIX + userId);
    JwtInformation information = toInformation(value);

    return information != null
        && accessTokenId.equals(information.getAccessTokenId());
  }

  @Override
  public boolean rotate(
      UUID userId,
      String expectedRefreshTokenId,
      String newAccessTokenId,
      String newRefreshTokenId,
      Instant refreshExpiresAt
  ) {
    if (expectedRefreshTokenId == null || expectedRefreshTokenId.isBlank()) {
      return false;
    }

    Assert.hasText(newAccessTokenId, "Access Token ID가 필요합니다.");
    Assert.hasText(newRefreshTokenId, "Refresh Token ID가 필요합니다.");

    String key = KEY_PREFIX + userId;
    JwtInformation newInformation =
        new JwtInformation(newAccessTokenId, newRefreshTokenId);

    return executeIfMatches(
        key,
        information ->
            expectedRefreshTokenId.equals(information.getRefreshTokenId()),
        operations -> operations.opsForValue().set(
            key,
            newInformation,
            remainingTtl(refreshExpiresAt)
        )
    );
  }

  @Override
  public boolean invalidate(UUID userId, String tokenId) {
    if (tokenId == null || tokenId.isBlank()) {
      return false;
    }

    String key = KEY_PREFIX + userId;

    return executeIfMatches(
        key,
        information ->
            tokenId.equals(information.getAccessTokenId())
                || tokenId.equals(information.getRefreshTokenId()),
        operations -> operations.delete(key)
    );
  }

  @Override
  public void invalidateByUserId(UUID userId) {
    redisTemplate.delete(KEY_PREFIX + userId);
  }

  @Nullable
  private JwtInformation toInformation(@Nullable Object value) {
    if (value == null) {
      return null;
    }

    return objectMapper.convertValue(value, JwtInformation.class);
  }

  private Duration remainingTtl(Instant expiresAt) {
    Assert.notNull(expiresAt, "Refresh Token 만료 시각이 필요합니다.");

    Duration ttl = Duration.between(Instant.now(), expiresAt);
    Assert.isTrue(
        ttl.toMillis() > 0,
        "Refresh Token 만료 시각은 현재보다 이후여야 합니다."
    );

    return ttl;
  }

  // 토큰을 확인한 뒤 교체·삭제하기 전에 로그인 정보가 바뀌면 작업을 취소한다.
  private boolean executeIfMatches(
      String key,
      Predicate<JwtInformation> condition,
      Consumer<RedisOperations<String, Object>> action
  ) {
    Boolean result = redisTemplate.execute(new SessionCallback<Boolean>() {

      @Override
      @SuppressWarnings("unchecked")
      public <K, V> Boolean execute(
          @NotNull RedisOperations<K, V> operations
      ) {
        RedisOperations<String, Object> redisOperations =
            (RedisOperations<String, Object>) (RedisOperations<?, ?>) operations;

        boolean transactionStarted = false;

        try {
          redisOperations.watch(key);

          JwtInformation information =
              toInformation(redisOperations.opsForValue().get(key));

          if (information == null || !condition.test(information)) {
            redisOperations.unwatch();
            return false;
          }

          redisOperations.multi();
          transactionStarted = true;

          action.accept(redisOperations);

          List<Object> results = redisOperations.exec();
          transactionStarted = false;

          if (results == null || results.isEmpty()) {
            return false;
          }

          for (Object commandResult : results) {
            if (commandResult instanceof RuntimeException exception) {
              throw exception;
            }
          }

          return true;
        } catch (RuntimeException exception) {
          try {
            if (transactionStarted) {
              redisOperations.discard();
            } else {
              redisOperations.unwatch();
            }
          } catch (RuntimeException cleanupException) {
            exception.addSuppressed(cleanupException);
          }

          throw exception;
        }
      }
    });

    return Boolean.TRUE.equals(result);
  }

}
