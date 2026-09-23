package com.moduplaylist.realtime.watchparty.redis;

import com.moduplaylist.realtime.watchparty.WatchPartyKickedRegistry;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisWatchPartyKickedRegistry implements WatchPartyKickedRegistry {

    private final StringRedisTemplate redisTemplate;

    public RedisWatchPartyKickedRegistry(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean isKicked(UUID partyId, UUID userId) {
        if (partyId == null || userId == null) {
            return false;
        }

        String key = WatchPartyRedisKey.kicked(partyId);
        Boolean isMember = redisTemplate.opsForSet().isMember(
                key, WatchPartyRedisKey.uuid(userId));
        return Boolean.TRUE.equals(isMember);
    }
}
