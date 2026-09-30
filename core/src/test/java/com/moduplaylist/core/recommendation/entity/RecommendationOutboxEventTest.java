package com.moduplaylist.core.recommendation.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecommendationOutboxEventTest {

    @Test
    void createsPendingInitialPreferenceEvent() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        RecommendationOutboxEvent event =
                RecommendationOutboxEvent.pendingInitialPreference(eventId, userId);

        assertThat(event.getEventId()).isEqualTo(eventId);
        assertThat(event.getUserId()).isEqualTo(userId);
        assertThat(event.getEventType())
                .isEqualTo(RecommendationOutboxEventType.INITIAL_PREFERENCE_CREATED);
        assertThat(event.getStatus()).isEqualTo(RecommendationOutboxStatus.PENDING);
        assertThat(event.getRetryCount()).isZero();
        assertThat(event.getNextRetryAt()).isNull();
        assertThat(event.getProcessingStartedAt()).isNull();
        assertThat(event.getClaimToken()).isNull();
        assertThat(event.getLastError()).isNull();
    }
}
