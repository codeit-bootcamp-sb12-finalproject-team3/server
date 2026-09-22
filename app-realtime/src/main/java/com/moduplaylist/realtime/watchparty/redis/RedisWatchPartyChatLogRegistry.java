package com.moduplaylist.realtime.watchparty.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moduplaylist.realtime.watchparty.WatchPartyChatLogRegistry;
import com.moduplaylist.realtime.watchparty.dto.WatchPartyChatMessage;
import java.util.UUID;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisWatchPartyChatLogRegistry implements WatchPartyChatLogRegistry {

    static final String KEY_PREFIX = "watchparty:";
    static final String KEY_SUFFIX = ":chat:log";
    private static final long MAX_LOG_SIZE = 500;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisWatchPartyChatLogRegistry(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void append(UUID partyId, WatchPartyChatMessage message) {
        String key = KEY_PREFIX + partyId + KEY_SUFFIX;
        String json = serialize(message);

        ListOperations<String, String> listOps = redisTemplate.opsForList();
        listOps.rightPush(key, json);
        listOps.trim(key, -MAX_LOG_SIZE, -1);
    }

    private String serialize(WatchPartyChatMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("채팅 메시지 직렬화에 실패했습니다.", e);
        }
    }
}