package com.moduplaylist.realtime.watchparty.redis;

import com.moduplaylist.realtime.watchparty.WatchPartyChatCooldownRegistry;
import java.time.Duration;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisWatchPartyChatCooldownRegistry implements WatchPartyChatCooldownRegistry {

    private final StringRedisTemplate redisTemplate;

    public RedisWatchPartyChatCooldownRegistry(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean tryAcquire(UUID partyId, UUID userId, Duration cooldown) {
        String key = WatchPartyRedisKey.chatCooldown(partyId, userId);
        // SET key "1" NX EX — 키가 없을 때만 저장하고 cooldown 뒤 자동 삭제
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, "1", cooldown);
        return Boolean.TRUE.equals(acquired);
    }
}