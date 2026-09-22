package com.moduplaylist.api.playlist.event;

import com.moduplaylist.core.activity.enums.ContentActivityType;
import java.time.Instant;
import java.util.UUID;

public record PlaylistContentActivityEvent(
    UUID eventId,
    ContentActivityType eventType,
    UUID userId,
    UUID contentId,
    Instant occurredAt
) {
}
