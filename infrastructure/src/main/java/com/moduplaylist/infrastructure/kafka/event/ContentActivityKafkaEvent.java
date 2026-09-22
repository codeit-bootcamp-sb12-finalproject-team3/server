package com.moduplaylist.infrastructure.kafka.event;

import com.moduplaylist.core.activity.enums.ContentActivityType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ContentActivityKafkaEvent(
        UUID eventId,
        ContentActivityType eventType,
        UUID userId,
        UUID contentId,
        BigDecimal oldRating,
        BigDecimal newRating,
        Instant occurredAt
) {

    public ContentActivityKafkaEvent(
            UUID eventId,
            ContentActivityType eventType,
            UUID userId,
            UUID contentId,
            Instant occurredAt
    ) {
        this(eventId, eventType, userId, contentId, null, null, occurredAt);
    }
}
