package com.moduplaylist.api.recommendation.event;

import java.time.Instant;
import java.util.UUID;

public record InitialPreferenceCreatedEvent(
        UUID eventId,
        UUID userId,
        Instant occurredAt
) {
}
