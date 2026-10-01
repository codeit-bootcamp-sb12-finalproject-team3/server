package com.moduplaylist.realtime.watchparty.redis;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class RedisWatchPartyChatCooldownRegistryTest {

    private static final Duration COOLDOWN = Duration.ofSeconds(1);

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOps = mock(ValueOperations.class);
    private final RedisWatchPartyChatCooldownRegistry registry = new RedisWatchPartyChatCooldownRegistry(redisTemplate);

    // 케이스 1: 키가 없으면(SET NX 성공) true
    @Test
    void tryAcquire_쿨다운_없음_true() {
        UUID partyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String key = "watchparty:" + partyId + ":chat-cooldown:" + userId;
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.setIfAbsent(key, "1", COOLDOWN)).willReturn(true);

        assertThat(registry.tryAcquire(partyId, userId, COOLDOWN)).isTrue();
    }

    // 케이스 2: 키가 이미 있으면(쿨다운 중) false
    @Test
    void tryAcquire_쿨다운_중_false() {
        UUID partyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String key = "watchparty:" + partyId + ":chat-cooldown:" + userId;
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.setIfAbsent(key, "1", COOLDOWN)).willReturn(false);

        assertThat(registry.tryAcquire(partyId, userId, COOLDOWN)).isFalse();
    }

    // 케이스 3: Redis가 null을 돌려줘도 false (안전하게 막음)
    @Test
    void tryAcquire_결과_null_false() {
        UUID partyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String key = "watchparty:" + partyId + ":chat-cooldown:" + userId;
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.setIfAbsent(key, "1", COOLDOWN)).willReturn(null);

        assertThat(registry.tryAcquire(partyId, userId, COOLDOWN)).isFalse();
    }
}