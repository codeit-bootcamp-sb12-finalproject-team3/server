package com.moduplaylist.realtime.watchparty.redis;

import com.moduplaylist.realtime.watchparty.WatchPartyJoinedRegistry;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisWatchPartyJoinedRegistry implements WatchPartyJoinedRegistry {

    private final StringRedisTemplate redisTemplate;

    public RedisWatchPartyJoinedRegistry(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean isJoined(UUID partyId, UUID userId) {
        if (partyId == null || userId == null) {
            return false;
        }

        String key = WatchPartyRedisKey.joined(partyId);
        Boolean isMember = redisTemplate.opsForSet().isMember(
                key, WatchPartyRedisKey.uuid(userId));
        return Boolean.TRUE.equals(isMember);
    }
}
