package com.moduplaylist.api.review.event;

import com.moduplaylist.core.activity.enums.ContentActivityType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ReviewRatingChangedEvent(
	UUID eventId,
	ContentActivityType eventType,
	UUID userId,
	UUID contentId,
	BigDecimal oldRating,
	BigDecimal newRating,
	Instant occurredAt
) {
}
