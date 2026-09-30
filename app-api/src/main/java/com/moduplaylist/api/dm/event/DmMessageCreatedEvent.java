package com.moduplaylist.api.dm.event;

import java.time.Instant;
import java.util.UUID;

public record DmMessageCreatedEvent(
        UUID messageId,
        UUID conversationId,
        UUID senderId,
        UUID receiverId,
        String content,
        Instant createdAt
) {
}
