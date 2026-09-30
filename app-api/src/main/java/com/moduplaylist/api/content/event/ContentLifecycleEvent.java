package com.moduplaylist.api.content.event;

import java.time.Instant;
import java.util.UUID;

public record ContentLifecycleEvent(
	UUID eventId,
	Type type,
	UUID contentId,
	Instant occurredAt
) {

	public enum Type {
		UPSERTED,
		DELETED
	}
}
