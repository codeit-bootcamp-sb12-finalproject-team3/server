package com.moduplaylist.realtime.dm.redis;

import java.time.Instant;
import java.util.UUID;

public record DmRedisMessage(
        UUID messageId,
        UUID conversationId,
        UUID senderId,
        UUID receiverId,
        String content,
        Instant createdAt
) {
}
