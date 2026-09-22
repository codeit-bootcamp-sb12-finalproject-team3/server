package com.moduplaylist.realtime.watchparty.redis;

import com.moduplaylist.realtime.watchparty.WatchPartyOnlineRegistry;
import java.util.List;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
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
        redisTemplate.opsForHash().increment(key, userId.toString(), 1);
    }

    private static final String DELETE_IF_ZERO_SCRIPT =
            "local current = redis.call('HGET', KEYS[1], ARGV[1]) " +
                    "if current and tonumber(current) <= 0 then " +
                    "  return redis.call('HDEL', KEYS[1], ARGV[1]) " +
                    "else return 0 end";

    @Override
    public void removeOnline(UUID partyId, UUID userId) {
        String key = KEY_PREFIX + partyId + KEY_SUFFIX;
        Long remaining = redisTemplate.opsForHash().increment(key, userId.toString(), -1);
        if (remaining == null || remaining <= 0) {
            redisTemplate.execute(
                    new DefaultRedisScript<>(DELETE_IF_ZERO_SCRIPT, Long.class),
                    List.of(key), userId.toString());        }
    }
}