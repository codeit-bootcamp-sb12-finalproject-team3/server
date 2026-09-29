package com.moduplaylist.api.recommendation.service;

import com.moduplaylist.core.recommendation.entity.RecommendationOutboxEventType;
import java.util.UUID;

public record RecommendationOutboxClaim(
        UUID id,
        UUID eventId,
        RecommendationOutboxEventType eventType,
        UUID userId,
        UUID claimToken
) {
}
