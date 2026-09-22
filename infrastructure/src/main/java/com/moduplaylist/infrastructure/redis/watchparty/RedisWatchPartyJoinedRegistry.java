package com.moduplaylist.infrastructure.redis.watchparty;

import com.moduplaylist.core.watchparty.repository.WatchPartyJoinedRegistry;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
@RequiredArgsConstructor
public class RedisWatchPartyJoinedRegistry implements WatchPartyJoinedRegistry {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void join(UUID partyId, UUID userId) {
        Assert.notNull(partyId, "partyId가 필요합니다.");
        Assert.notNull(userId, "userId가 필요합니다.");

        String key = WatchPartyRedisKey.joined(partyId);
        redisTemplate.opsForSet().add(key, userId.toString());
    }

    @Override
    public void leave(UUID partyId, UUID userId) {
        Assert.notNull(partyId, "partyId가 필요합니다.");
        Assert.notNull(userId, "userId가 필요합니다.");

        String key = WatchPartyRedisKey.joined(partyId);
        redisTemplate.opsForSet().remove(key, userId.toString());
    }

    @Override
    public Set<UUID> findAll(UUID partyId) {
        Assert.notNull(partyId, "partyId가 필요합니다.");

        String key = WatchPartyRedisKey.joined(partyId);
        Set<Object> members = redisTemplate.opsForSet().members(key);
        if (members == null) {
            return Set.of();
        }
        return members.stream()
                .map(Object::toString)
                .map(UUID::fromString)
                .collect(Collectors.toSet());
    }
}