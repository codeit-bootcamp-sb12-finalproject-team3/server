package com.moduplaylist.infrastructure.redis.watchparty;

import com.moduplaylist.core.watchparty.repository.WatchPartyHostRegistry;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
@RequiredArgsConstructor
public class RedisWatchPartyHostRegistry implements WatchPartyHostRegistry {

    private final StringRedisTemplate redisTemplate;

    @Override
    public void setHost(UUID partyId, UUID hostId) {
        Assert.notNull(partyId, "partyId가 필요합니다.");
        Assert.notNull(hostId, "hostId가 필요합니다.");

        String key = WatchPartyRedisKey.host(partyId);
        redisTemplate.opsForValue().set(key, WatchPartyRedisKey.uuid(hostId));
    }
}
