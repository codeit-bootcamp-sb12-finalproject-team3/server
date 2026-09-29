package com.moduplaylist.realtime.dm.presence;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class RedisDmActiveConversationRegistry implements DmActiveConversationRegistry {

    static final Duration ACTIVE_TTL = Duration.ofHours(1);

    private static final String DEACTIVATE_SCRIPT =
            "redis.call('SREM', KEYS[1], ARGV[1]) "
                    + "local remaining = redis.call('SCARD', KEYS[1]) "
                    + "if remaining == 0 then redis.call('DEL', KEYS[1]) end "
                    + "return remaining";

    private final StringRedisTemplate redisTemplate;

    public RedisDmActiveConversationRegistry(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void activate(UUID userId, UUID conversationId, String sessionId) {
        validate(userId, conversationId, sessionId);
        String key = DmRedisKey.activeConversation(userId, conversationId);
        redisTemplate.opsForSet().add(key, sessionId);
        redisTemplate.expire(key, ACTIVE_TTL);
    }

    @Override
    public void deactivate(UUID userId, UUID conversationId, String sessionId) {
        validate(userId, conversationId, sessionId);
        String key = DmRedisKey.activeConversation(userId, conversationId);
        redisTemplate.execute(
                new DefaultRedisScript<>(DEACTIVATE_SCRIPT, Long.class),
                List.of(key),
                sessionId
        );
    }

    private void validate(UUID userId, UUID conversationId, String sessionId) {
        Assert.notNull(userId, "userId가 필요합니다.");
        Assert.notNull(conversationId, "conversationId가 필요합니다.");
        Assert.hasText(sessionId, "sessionId가 필요합니다.");
    }
}
