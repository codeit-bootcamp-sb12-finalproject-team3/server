package com.moduplaylist.infrastructure.kafka.event;

import com.moduplaylist.core.activity.enums.ContentActivityType;
import java.time.Instant;
import java.util.UUID;

public record ContentViewed(
	UUID eventId,
	ContentActivityType eventType,
	UUID userId,
	UUID contentId,
	Instant occurredAt
) {
}
