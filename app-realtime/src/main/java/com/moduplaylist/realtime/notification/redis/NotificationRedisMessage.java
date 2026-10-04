package com.moduplaylist.realtime.notification.redis;

import java.time.Instant;
import java.util.UUID;

public record NotificationRedisMessage(
        UUID receiverId,
        UUID notificationId,
        String title,
        String content,
        String level,
        Instant createdAt
) {
}
