package com.moduplaylist.infrastructure.redis.watchparty;

import com.moduplaylist.core.watchparty.repository.WatchPartyLastSeenRegistry;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
@RequiredArgsConstructor
public class RedisWatchPartyLastSeenRegistry implements WatchPartyLastSeenRegistry {

    private final StringRedisTemplate redisTemplate;

    @Override
    public Optional<Instant> findLastSeen(UUID partyId, UUID userId) {
        Assert.notNull(partyId, "partyId가 필요합니다.");
        Assert.notNull(userId, "userId가 필요합니다.");

        Object value = redisTemplate.opsForHash()
                .get(WatchPartyRedisKey.lastSeen(partyId), WatchPartyRedisKey.uuid(userId));
        return Optional.ofNullable(value).map(RedisWatchPartyLastSeenRegistry::toInstant);
    }

    @Override
    public Map<UUID, Instant> findAllLastSeen(UUID partyId) {
        Assert.notNull(partyId, "partyId가 필요합니다.");

        Map<Object, Object> entries = redisTemplate.opsForHash()
                .entries(WatchPartyRedisKey.lastSeen(partyId));
        Map<UUID, Instant> result = new HashMap<>();
        entries.forEach((field, value) ->
                result.put(WatchPartyRedisKey.parseUuid((String) field), toInstant(value)));
        return result;
    }

    // realtime이 epoch millis 문자열로 기록함 (realtime RedisWatchPartyLastSeenRegistry와 짝)
    private static Instant toInstant(Object value) {
        return Instant.ofEpochMilli(Long.parseLong((String) value));
    }
}