package com.moduplaylist.infrastructure.redis.watchparty;

import com.moduplaylist.core.watchparty.repository.WatchPartyKeyLifecycleRegistry;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
@RequiredArgsConstructor
public class RedisWatchPartyKeyLifecycleRegistry implements WatchPartyKeyLifecycleRegistry {

    private static final Duration SAFETY_NET_TTL = Duration.ofDays(15);

    // 보관 가치가 있는 데이터만 TTL로 유지
    private static final List<Function<UUID, String>> RETAINED_KEY_BUILDERS = List.of(
            WatchPartyRedisKey::playback,
            WatchPartyRedisKey::chatLog
    );

    // 권한/실시간 상태용 키는 종료 즉시 제거 (TTL까지 기다릴 필요 없음)
    private static final List<Function<UUID, String>> IMMEDIATE_KEY_BUILDERS = List.of(
            WatchPartyRedisKey::online,
            WatchPartyRedisKey::kicked,
            WatchPartyRedisKey::joined,
            WatchPartyRedisKey::host
    );

    private static final List<Function<UUID, String>> ALL_KEY_BUILDERS =
            Stream.concat(RETAINED_KEY_BUILDERS.stream(), IMMEDIATE_KEY_BUILDERS.stream()).toList();

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void armSafetyNetTtl(UUID partyId) {
        Assert.notNull(partyId, "partyId가 필요합니다.");

        keysOf(partyId, RETAINED_KEY_BUILDERS)
                .forEach(key -> redisTemplate.expire(key, SAFETY_NET_TTL));
        redisTemplate.delete(keysOf(partyId, IMMEDIATE_KEY_BUILDERS));
    }

    @Override
    public void deletePartyKeysNow(UUID partyId) {
        Assert.notNull(partyId, "partyId가 필요합니다.");
        redisTemplate.delete(keysOf(partyId, ALL_KEY_BUILDERS));
    }

    private List<String> keysOf(UUID partyId, List<Function<UUID, String>> builders) {
        return builders.stream()
                .map(builder -> builder.apply(partyId))
                .toList();
    }
}