package com.moduplaylist.realtime.watchparty.redis;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * realtime(쓰기) → app-api(읽기) lastSeen 규격 중 "쓰는 쪽" 검증.
 * infrastructure WatchPartyLastSeenContractTest와 키·값 형식이 같아야 한다.
 */
class RedisWatchPartyLastSeenRegistryTest {

    // 테스트용 고정 시각: 2026-09-21T14:13:20Z (epoch millis, realtime이 Redis에 쓰는 형식)
    private static final long SEEN_AT_MILLIS = 1_790_000_000_000L;

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
    private final RedisWatchPartyLastSeenRegistry registry = new RedisWatchPartyLastSeenRegistry(redisTemplate);

    @Test
    void touch는_파티_키에_userId_필드로_epoch_millis_문자열을_쓰고_TTL을_연장한다() {
        UUID partyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String key = "watchparty:" + partyId + ":lastSeen";
        given(redisTemplate.<Object, Object>opsForHash()).willReturn(hashOps);

        registry.touch(partyId, userId, SEEN_AT_MILLIS);

        verify(hashOps).put(key, userId.toString(), String.valueOf(SEEN_AT_MILLIS));
        verify(redisTemplate).expire(key, Duration.ofHours(1));
    }

    @Test
    void touchAll은_파티마다_한_번에_기록한다() {
        UUID partyId = UUID.randomUUID();
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();
        String key = "watchparty:" + partyId + ":lastSeen";
        given(redisTemplate.<Object, Object>opsForHash()).willReturn(hashOps);

        registry.touchAll(Map.of(partyId, Set.of(userA, userB)), SEEN_AT_MILLIS);

        verify(hashOps).putAll(key, Map.of(
                userA.toString(), String.valueOf(SEEN_AT_MILLIS),
                userB.toString(), String.valueOf(SEEN_AT_MILLIS)));
        verify(redisTemplate).expire(key, Duration.ofHours(1));
    }
}