package com.moduplaylist.infrastructure.redis.watchparty;

import com.moduplaylist.core.watchparty.repository.WatchPartyLifecycleRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RedisWatchPartyLifecycleRegistry implements WatchPartyLifecycleRegistry {

    // 안전장치 TTL 목표값
    private static final Duration SAFETY_NET_TTL = Duration.ofDays(15);
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void armSafetyNetTtl(UUID partyId) {
        partyKeys(partyId).forEach(key -> redisTemplate.expire(key, SAFETY_NET_TTL));
    }

    @Override
    public void deletePartyKeysNow(UUID partyId) {
        redisTemplate.delete(partyKeys(partyId));
    }

    private List<String> partyKeys(UUID partyId) {
        return List.of(
                WatchPartyRedisKey.playback(partyId),
                WatchPartyRedisKey.online(partyId),
                WatchPartyRedisKey.kicked(partyId),
                WatchPartyRedisKey.host(partyId),
                WatchPartyRedisKey.joined(partyId),
                WatchPartyRedisKey.chatLog(partyId)
        );
    }
}