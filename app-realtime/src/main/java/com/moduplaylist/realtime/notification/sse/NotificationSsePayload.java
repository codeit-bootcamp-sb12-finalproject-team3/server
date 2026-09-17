package com.moduplaylist.realtime.notification.sse;


import java.time.Instant;
import java.util.UUID;

public record NotificationSsePayload(
        UUID id,
        String title,
        String content,
        String level,
        Instant createdAt
) {
}
