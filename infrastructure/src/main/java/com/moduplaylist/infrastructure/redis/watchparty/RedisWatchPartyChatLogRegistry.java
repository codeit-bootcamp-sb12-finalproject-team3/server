package com.moduplaylist.infrastructure.redis.watchparty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moduplaylist.core.watchparty.repository.WatchPartyChatLogRegistry;
import com.moduplaylist.core.watchparty.repository.WatchPartyChatMessage;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
@RequiredArgsConstructor
public class RedisWatchPartyChatLogRegistry implements WatchPartyChatLogRegistry {

    private static final long MAX_LOG_SIZE = 500;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void append(UUID partyId, WatchPartyChatMessage message) {
        Assert.notNull(partyId, "partyId가 필요합니다.");
        Assert.notNull(message, "message가 필요합니다.");

        String key = WatchPartyRedisKey.chatLog(partyId);
        ListOperations<String, Object> listOps = redisTemplate.opsForList();
        listOps.rightPush(key, message);
        listOps.trim(key, -MAX_LOG_SIZE, -1);

    }

    @Override
    public List<WatchPartyChatMessage> findRecent(UUID partyId, long count) {
        Assert.notNull(partyId, "partyId가 필요합니다.");
        Assert.isTrue(count > 0, "count는 1 이상이어야 합니다.");

        String key = WatchPartyRedisKey.chatLog(partyId);
        ListOperations<String, Object> listOps = redisTemplate.opsForList();
        List<Object> raw = listOps.range(key, -count, -1);

        if (raw == null) {
            return List.of();
        }
        return raw.stream()
                .map(item -> objectMapper.convertValue(item, WatchPartyChatMessage.class))
                .toList();
    }
}