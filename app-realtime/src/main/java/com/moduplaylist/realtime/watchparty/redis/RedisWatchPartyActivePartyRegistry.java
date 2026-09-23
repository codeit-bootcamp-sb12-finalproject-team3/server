package com.moduplaylist.realtime.watchparty.redis;

import com.moduplaylist.realtime.watchparty.WatchPartyActivePartyRegistry;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisWatchPartyActivePartyRegistry implements WatchPartyActivePartyRegistry {

    private final StringRedisTemplate redisTemplate;

    public RedisWatchPartyActivePartyRegistry(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Optional<UUID> findJoinedPartyId(UUID userId) {
        if (userId == null) {
            return Optional.empty();
        }

        String key = WatchPartyRedisKey.joinedParty(userId);
        String partyId = redisTemplate.opsForValue().get(key);
        if (partyId == null || partyId.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(WatchPartyRedisKey.parseUuid(partyId));
    }
}
