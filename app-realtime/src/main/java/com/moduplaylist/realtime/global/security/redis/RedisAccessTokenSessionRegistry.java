package com.moduplaylist.realtime.global.security.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moduplaylist.realtime.global.security.AccessTokenSessionRegistry;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisAccessTokenSessionRegistry implements AccessTokenSessionRegistry {

    static final String KEY_PREFIX = "auth:jwt:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisAccessTokenSessionRegistry(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean isAccessTokenActive(UUID userId, String accessTokenId) {
        if (userId == null || accessTokenId == null || accessTokenId.isBlank()) {
            return false;
        }

        String storedJson = redisTemplate.opsForValue().get(KEY_PREFIX + userId);
        if (storedJson == null || storedJson.isBlank()) {
            return false;
        }

        try {
            RealtimeJwtSession session =
                    objectMapper.readValue(storedJson, RealtimeJwtSession.class);
            return accessTokenId.equals(session.accessTokenId());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to read the Redis JWT session contract.", exception);
        }
    }
}
