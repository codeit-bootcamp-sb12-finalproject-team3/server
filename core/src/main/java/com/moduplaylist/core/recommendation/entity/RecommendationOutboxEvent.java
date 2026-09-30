package com.moduplaylist.core.recommendation.entity;

import com.moduplaylist.core.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "recommendation_outbox_events",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_recommendation_outbox_events_event_id",
                columnNames = "event_id"
        ),
        indexes = {
                @Index(
                        name = "idx_recommendation_outbox_events_due",
                        columnList = "status, next_retry_at, created_at"
                ),
                @Index(
                        name = "idx_recommendation_outbox_events_stale",
                        columnList = "status, processing_started_at"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendationOutboxEvent extends BaseEntity {

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "event_type",
            nullable = false,
            updatable = false,
            columnDefinition = "ENUM('INITIAL_PREFERENCE_CREATED')"
    )
    private RecommendationOutboxEventType eventType;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            columnDefinition = "ENUM('PENDING','PROCESSING','COMPLETED','FAILED')"
    )
    private RecommendationOutboxStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "processing_started_at")
    private Instant processingStartedAt;

    @Column(name = "claim_token")
    private UUID claimToken;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    private RecommendationOutboxEvent(
            UUID eventId,
            RecommendationOutboxEventType eventType,
            UUID userId
    ) {
        this.eventId = Objects.requireNonNull(eventId);
        this.eventType = Objects.requireNonNull(eventType);
        this.userId = Objects.requireNonNull(userId);
        this.status = RecommendationOutboxStatus.PENDING;
        this.retryCount = 0;
    }

    public static RecommendationOutboxEvent pendingInitialPreference(
            UUID eventId,
            UUID userId
    ) {
        return new RecommendationOutboxEvent(
                eventId,
                RecommendationOutboxEventType.INITIAL_PREFERENCE_CREATED,
                userId
        );
    }
}
