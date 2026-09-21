package com.moduplaylist.realtime.watchparty.redis;

import com.moduplaylist.realtime.watchparty.WatchPartyOnlineRegistry;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisWatchPartyOnlineRegistry implements WatchPartyOnlineRegistry {

    static final String KEY_PREFIX = "watchparty:";
    static final String KEY_SUFFIX = ":online";

    private final StringRedisTemplate redisTemplate;

    public RedisWatchPartyOnlineRegistry(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void addOnline(UUID partyId, UUID userId) {
        String key = KEY_PREFIX + partyId + KEY_SUFFIX;
        redisTemplate.opsForSet().add(key, userId.toString());
    }

    @Override
    public void removeOnline(UUID partyId, UUID userId) {
        String key = KEY_PREFIX + partyId + KEY_SUFFIX;
        redisTemplate.opsForSet().remove(key, userId.toString());
    }
}