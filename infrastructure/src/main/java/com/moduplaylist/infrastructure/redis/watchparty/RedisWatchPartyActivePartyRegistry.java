package com.moduplaylist.infrastructure.redis.watchparty;

import com.moduplaylist.core.watchparty.repository.WatchPartyActivePartyRegistry;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
@RequiredArgsConstructor
public class RedisWatchPartyActivePartyRegistry implements WatchPartyActivePartyRegistry {

    private final StringRedisTemplate redisTemplate;

    @Override
    public void setJoinedParty(UUID userId, UUID partyId) {
        Assert.notNull(userId, "userId가 필요합니다.");
        Assert.notNull(partyId, "partyId가 필요합니다.");

        String key = WatchPartyRedisKey.joinedParty(userId);
        redisTemplate.opsForValue().set(key, WatchPartyRedisKey.uuid(partyId));
    }

    @Override
    public void clearJoinedParty(UUID userId) {
        Assert.notNull(userId, "userId가 필요합니다.");

        String key = WatchPartyRedisKey.joinedParty(userId);
        redisTemplate.delete(key);
    }
}
