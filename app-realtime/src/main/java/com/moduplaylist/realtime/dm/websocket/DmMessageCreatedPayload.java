package com.moduplaylist.realtime.dm.websocket;

import java.time.Instant;
import java.util.UUID;

public record DmMessageCreatedPayload(
        UUID messageId,
        UUID conversationId,
        UUID senderId,
        UUID receiverId,
        String content,
        Instant createdAt
) {
}
