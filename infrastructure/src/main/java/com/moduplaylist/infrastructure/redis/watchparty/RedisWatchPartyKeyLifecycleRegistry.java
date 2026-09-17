package com.moduplaylist.infrastructure.redis.watchparty;

import com.moduplaylist.core.watchparty.repository.WatchPartyKeyLifecycleRegistry;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
@RequiredArgsConstructor
public class RedisWatchPartyKeyLifecycleRegistry implements WatchPartyKeyLifecycleRegistry {

    private static final Duration SAFETY_NET_TTL = Duration.ofDays(15);

    private static final List<Function<UUID, String>> KEY_BUILDERS = List.of(
            WatchPartyRedisKey::playback,
            WatchPartyRedisKey::online,
            WatchPartyRedisKey::kicked,
            WatchPartyRedisKey::chatLog
    );

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void armSafetyNetTtl(UUID partyId) {
        Assert.notNull(partyId, "partyId가 필요합니다.");
        keysOf(partyId).forEach(key -> redisTemplate.expire(key, SAFETY_NET_TTL));
    }

    @Override
    public void deletePartyKeysNow(UUID partyId) {
        Assert.notNull(partyId, "partyId가 필요합니다.");
        redisTemplate.delete(keysOf(partyId));
    }

    private List<String> keysOf(UUID partyId) {
        return KEY_BUILDERS.stream()
                .map(builder -> builder.apply(partyId))
                .toList();
    }
}