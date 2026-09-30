package com.moduplaylist.api.notification.event;

import com.moduplaylist.core.notification.entity.NotificationLevel;
import java.time.Instant;
import java.util.UUID;

public record NotificationCreatedEvent(
        UUID notificationId,
        UUID receiverId,
        String title,
        String content,
        NotificationLevel level,
        Instant createdAt
) {
}