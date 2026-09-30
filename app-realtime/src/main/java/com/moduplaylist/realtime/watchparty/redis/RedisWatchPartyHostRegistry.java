package com.moduplaylist.realtime.watchparty.redis;

import com.moduplaylist.realtime.watchparty.WatchPartyHostRegistry;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisWatchPartyHostRegistry implements WatchPartyHostRegistry {

    private final StringRedisTemplate redisTemplate;

    public RedisWatchPartyHostRegistry(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean isHost(UUID partyId, UUID userId) {
        if (partyId == null || userId == null) {
            return false;
        }

        String key = WatchPartyRedisKey.host(partyId);
        String hostId = redisTemplate.opsForValue().get(key);
        return WatchPartyRedisKey.uuid(userId).equals(hostId);
    }
}
