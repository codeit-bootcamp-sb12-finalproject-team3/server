package com.moduplaylist.realtime.watchparty.redis;

import com.moduplaylist.realtime.watchparty.WatchPartyJoinedRegistry;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisWatchPartyJoinedRegistry implements WatchPartyJoinedRegistry {

    static final String KEY_PREFIX = "watchparty:";
    static final String KEY_SUFFIX = ":joined";

    private final StringRedisTemplate redisTemplate;

    public RedisWatchPartyJoinedRegistry(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean isJoined(UUID partyId, UUID userId) {
        if (partyId == null || userId == null) {
            return false;
        }

        String key = KEY_PREFIX + partyId + KEY_SUFFIX;
        Boolean isMember = redisTemplate.opsForSet().isMember(key, userId.toString());
        return Boolean.TRUE.equals(isMember);
    }
}