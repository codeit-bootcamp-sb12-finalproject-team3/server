package com.moduplaylist.infrastructure.kafka.event;

import java.time.Instant;
import java.util.UUID;

public record NotificationCreatedKafkaEvent(
        UUID notificationId,
        UUID receiverId,
        String title,
        String content,
        String level,
        Instant createdAt
) {
}