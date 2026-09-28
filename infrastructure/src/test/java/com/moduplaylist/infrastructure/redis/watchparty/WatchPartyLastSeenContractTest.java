package com.moduplaylist.infrastructure.redis.watchparty;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * realtime(쓰기) → app-api(읽기) lastSeen 규격 중 "읽는 쪽" 검증.
 * realtime RedisWatchPartyLastSeenRegistryTest와 키·값 형식이 같아야 한다.
 */
class WatchPartyLastSeenContractTest {

    // 테스트용 고정 시각: 2026-09-21T14:13:20Z (epoch millis, realtime이 Redis에 쓰는 형식)
    private static final long SEEN_AT_MILLIS = 1_790_000_000_000L;

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
    private final RedisWatchPartyLastSeenRegistry registry = new RedisWatchPartyLastSeenRegistry(redisTemplate);

    @Test
    void 키_이름이_realtime이_쓰는_키와_같다() {
        UUID partyId = UUID.randomUUID();

        assertThat(WatchPartyRedisKey.lastSeen(partyId))
                .isEqualTo("watchparty:" + partyId + ":lastSeen");
    }

    @Test
    void realtime이_쓴_epoch_millis_문자열을_Instant로_읽는다() {
        UUID partyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        given(redisTemplate.<Object, Object>opsForHash()).willReturn(hashOps);
        given(hashOps.get("watchparty:" + partyId + ":lastSeen", userId.toString()))
                .willReturn(String.valueOf(SEEN_AT_MILLIS));

        assertThat(registry.findLastSeen(partyId, userId))
                .contains(Instant.ofEpochMilli(SEEN_AT_MILLIS));
    }

    @Test
    void 기록이_없으면_빈_Optional() {
        UUID partyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        given(redisTemplate.<Object, Object>opsForHash()).willReturn(hashOps);
        given(hashOps.get("watchparty:" + partyId + ":lastSeen", userId.toString())).willReturn(null);

        assertThat(registry.findLastSeen(partyId, userId)).isEmpty();
    }

    @Test
    void 파티_전체를_userId별_Instant로_읽는다() {
        UUID partyId = UUID.randomUUID();
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();
        given(redisTemplate.<Object, Object>opsForHash()).willReturn(hashOps);
        given(hashOps.entries("watchparty:" + partyId + ":lastSeen")).willReturn(Map.of(
                userA.toString(), String.valueOf(SEEN_AT_MILLIS),
                userB.toString(), String.valueOf(SEEN_AT_MILLIS + 60_000)));

        assertThat(registry.findAllLastSeen(partyId)).containsExactlyInAnyOrderEntriesOf(Map.of(
                userA, Instant.ofEpochMilli(SEEN_AT_MILLIS),
                userB, Instant.ofEpochMilli(SEEN_AT_MILLIS + 60_000)));
    }
}