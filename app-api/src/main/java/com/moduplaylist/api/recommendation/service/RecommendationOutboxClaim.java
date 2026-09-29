package com.moduplaylist.api.recommendation.service;

import com.moduplaylist.core.recommendation.entity.RecommendationOutboxEventType;
import java.time.Instant;
import java.util.UUID;

public record RecommendationOutboxClaim(
        UUID id,
        UUID eventId,
        RecommendationOutboxEventType eventType,
        UUID userId,
        Instant createdAt,
        int retryCount,
        UUID claimToken
) {
}
