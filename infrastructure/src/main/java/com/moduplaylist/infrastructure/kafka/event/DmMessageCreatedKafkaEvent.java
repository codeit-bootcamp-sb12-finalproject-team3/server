package com.moduplaylist.infrastructure.kafka.event;

import java.time.Instant;
import java.util.UUID;

public record DmMessageCreatedKafkaEvent(
        UUID messageId,
        UUID conversationId,
        UUID senderId,
        UUID receiverId,
        String content,
        Instant createdAt
) {
}
