package com.moduplaylist.infrastructure.redis.dm;

import com.moduplaylist.core.dm.repository.DmActiveConversationRegistry;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
@RequiredArgsConstructor
public class RedisDmActiveConversationRegistry implements DmActiveConversationRegistry {

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean isActive(UUID userId, UUID conversationId) {
        Assert.notNull(userId, "userId가 필요합니다.");
        Assert.notNull(conversationId, "conversationId가 필요합니다.");

        Long activeSessionCount = redisTemplate.opsForSet()
                .size(DmRedisKey.activeConversation(userId, conversationId));
        return activeSessionCount != null && activeSessionCount > 0;
    }
}
