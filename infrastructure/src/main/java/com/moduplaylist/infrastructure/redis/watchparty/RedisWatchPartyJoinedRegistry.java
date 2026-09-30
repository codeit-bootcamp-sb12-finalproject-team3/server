package com.moduplaylist.infrastructure.redis.watchparty;

import com.moduplaylist.core.watchparty.repository.WatchPartyJoinedRegistry;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
@RequiredArgsConstructor
public class RedisWatchPartyJoinedRegistry implements WatchPartyJoinedRegistry {

    private final StringRedisTemplate redisTemplate;

    @Override
    public void join(UUID partyId, UUID userId) {
        Assert.notNull(partyId, "partyId가 필요합니다.");
        Assert.notNull(userId, "userId가 필요합니다.");

        String key = WatchPartyRedisKey.joined(partyId);
        redisTemplate.opsForSet().add(key, WatchPartyRedisKey.uuid(userId));
    }

    @Override
    public void leave(UUID partyId, UUID userId) {
        Assert.notNull(partyId, "partyId가 필요합니다.");
        Assert.notNull(userId, "userId가 필요합니다.");

        String key = WatchPartyRedisKey.joined(partyId);
        redisTemplate.opsForSet().remove(key, WatchPartyRedisKey.uuid(userId));
    }

    @Override
    public Set<UUID> findAll(UUID partyId) {
        Assert.notNull(partyId, "partyId가 필요합니다.");

        String key = WatchPartyRedisKey.joined(partyId);
        Set<String> members = redisTemplate.opsForSet().members(key);
        if (members == null) {
            return Set.of();
        }
        return members.stream()
                .map(WatchPartyRedisKey::parseUuid)
                .collect(Collectors.toSet());
    }
}
