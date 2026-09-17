package com.moduplaylist.infrastructure.redis.watchparty;

import com.moduplaylist.core.watchparty.repository.WatchPartyOnlineRegistry;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
@RequiredArgsConstructor
public class RedisWatchPartyOnlineRegistry implements WatchPartyOnlineRegistry {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void addOnline(UUID partyId, UUID userId) {
        Assert.notNull(partyId, "partyId가 필요합니다.");
        Assert.notNull(userId, "userId가 필요합니다.");

        String key = WatchPartyRedisKey.online(partyId);
        redisTemplate.opsForSet().add(key, userId.toString());
    }

    @Override
    public void removeOnline(UUID partyId, UUID userId) {
        Assert.notNull(partyId, "partyId가 필요합니다.");
        Assert.notNull(userId, "userId가 필요합니다.");

        String key = WatchPartyRedisKey.online(partyId);
        redisTemplate.opsForSet().remove(key, userId.toString());
    }
}