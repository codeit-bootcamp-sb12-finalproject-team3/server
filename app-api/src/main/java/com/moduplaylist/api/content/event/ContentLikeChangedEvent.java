package com.moduplaylist.api.content.event;

import com.moduplaylist.core.activity.enums.ContentActivityType;
import java.time.Instant;
import java.util.UUID;

public record ContentLikeChangedEvent(
	UUID eventId,
	ContentActivityType eventType,
	UUID userId,
	UUID contentId,
	Instant occurredAt
) {
}
