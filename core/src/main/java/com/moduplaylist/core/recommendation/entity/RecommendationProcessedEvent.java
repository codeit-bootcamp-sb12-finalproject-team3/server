package com.moduplaylist.core.recommendation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "recommendation_processed_events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendationProcessedEvent {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(
            name = "processed_at",
            nullable = false,
            insertable = false,
            updatable = false
    )
    private Instant processedAt;
}
