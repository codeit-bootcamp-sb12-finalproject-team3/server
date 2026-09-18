package com.moduplaylist.infrastructure.kafka.event;

import java.time.Instant;
import java.util.UUID;

public record ContentViewed(
	UUID eventId,
	UUID userId,
	UUID contentId,
	Instant occurredAt
) {
}
