package com.moduplaylist.infrastructure.kafka.event;

import java.time.Instant;
import java.util.UUID;

public record ContentUpserted(
	UUID eventId,
	UUID contentId,
	Instant occurredAt
) {
}
