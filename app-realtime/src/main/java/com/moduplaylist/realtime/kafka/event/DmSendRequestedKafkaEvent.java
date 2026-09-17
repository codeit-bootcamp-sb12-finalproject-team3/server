package com.moduplaylist.realtime.kafka.event;

import java.util.UUID;

public record DmSendRequestedKafkaEvent(
        UUID conversationId,
        UUID senderId,
        String content,
        UUID clientMessageId
) {
}
