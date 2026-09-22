package com.moduplaylist.infrastructure.redis.watchparty;

import com.moduplaylist.core.watchparty.repository.WatchPartyKickedRegistry;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
@RequiredArgsConstructor
public class RedisWatchPartyKickedRegistry implements WatchPartyKickedRegistry {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void kick(UUID partyId, UUID userId) {
        Assert.notNull(partyId, "partyId가 필요합니다.");
        Assert.notNull(userId, "userId가 필요합니다.");

        String key = WatchPartyRedisKey.kicked(partyId);
        redisTemplate.opsForSet().add(key, userId.toString());
    }

    @Override
    public boolean isKicked(UUID partyId, UUID userId) {
        Assert.notNull(partyId, "partyId가 필요합니다.");
        Assert.notNull(userId, "userId가 필요합니다.");

        String key = WatchPartyRedisKey.kicked(partyId);
        Boolean result = redisTemplate.opsForSet().isMember(key, userId.toString());
        return Boolean.TRUE.equals(result);
    }
}