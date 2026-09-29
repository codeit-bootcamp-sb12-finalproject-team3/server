package com.moduplaylist.core.recommendation.repository;

import com.moduplaylist.core.recommendation.entity.RecommendationOutboxEvent;
import com.moduplaylist.core.recommendation.entity.RecommendationOutboxStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecommendationOutboxEventRepository
        extends JpaRepository<RecommendationOutboxEvent, UUID> {

    Optional<RecommendationOutboxEvent> findByEventId(UUID eventId);

    @Query("""
            select event
            from RecommendationOutboxEvent event
            where (
                event.status = :pendingStatus
                and (event.nextRetryAt is null or event.nextRetryAt <= :now)
            ) or (
                event.status = :processingStatus
                and event.processingStartedAt is not null
                and event.processingStartedAt <= :staleBefore
            )
            order by event.createdAt asc
            """)
    List<RecommendationOutboxEvent> findClaimableEvents(
            @Param("pendingStatus") RecommendationOutboxStatus pendingStatus,
            @Param("processingStatus") RecommendationOutboxStatus processingStatus,
            @Param("now") Instant now,
            @Param("staleBefore") Instant staleBefore,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RecommendationOutboxEvent event
            set event.status = :processingStatus,
                event.processingStartedAt = :claimedAt,
                event.claimToken = :claimToken,
                event.nextRetryAt = null,
                event.updatedAt = :claimedAt
            where event.id = :id
              and (
                  (
                      event.status = :pendingStatus
                      and (event.nextRetryAt is null or event.nextRetryAt <= :claimedAt)
                  ) or (
                      event.status = :processingStatus
                      and event.processingStartedAt is not null
                      and event.processingStartedAt <= :staleBefore
                  )
              )
            """)
    int tryClaim(
            @Param("id") UUID id,
            @Param("claimToken") UUID claimToken,
            @Param("claimedAt") Instant claimedAt,
            @Param("staleBefore") Instant staleBefore,
            @Param("pendingStatus") RecommendationOutboxStatus pendingStatus,
            @Param("processingStatus") RecommendationOutboxStatus processingStatus
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RecommendationOutboxEvent event
            set event.status = :completedStatus,
                event.processingStartedAt = null,
                event.claimToken = null,
                event.nextRetryAt = null,
                event.lastError = null,
                event.updatedAt = :completedAt
            where event.id = :id
              and event.status = :processingStatus
              and event.claimToken = :claimToken
            """)
    int complete(
            @Param("id") UUID id,
            @Param("claimToken") UUID claimToken,
            @Param("completedAt") Instant completedAt,
            @Param("processingStatus") RecommendationOutboxStatus processingStatus,
            @Param("completedStatus") RecommendationOutboxStatus completedStatus
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RecommendationOutboxEvent event
            set event.status = :pendingStatus,
                event.retryCount = event.retryCount + 1,
                event.nextRetryAt = :nextRetryAt,
                event.processingStartedAt = null,
                event.claimToken = null,
                event.lastError = :lastError,
                event.updatedAt = :failedAt
            where event.id = :id
              and event.status = :processingStatus
              and event.claimToken = :claimToken
            """)
    int retry(
            @Param("id") UUID id,
            @Param("claimToken") UUID claimToken,
            @Param("nextRetryAt") Instant nextRetryAt,
            @Param("lastError") String lastError,
            @Param("failedAt") Instant failedAt,
            @Param("processingStatus") RecommendationOutboxStatus processingStatus,
            @Param("pendingStatus") RecommendationOutboxStatus pendingStatus
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RecommendationOutboxEvent event
            set event.status = :failedStatus,
                event.nextRetryAt = null,
                event.processingStartedAt = null,
                event.claimToken = null,
                event.lastError = :lastError,
                event.updatedAt = :failedAt
            where event.id = :id
              and event.status = :processingStatus
              and event.claimToken = :claimToken
            """)
    int fail(
            @Param("id") UUID id,
            @Param("claimToken") UUID claimToken,
            @Param("lastError") String lastError,
            @Param("failedAt") Instant failedAt,
            @Param("processingStatus") RecommendationOutboxStatus processingStatus,
            @Param("failedStatus") RecommendationOutboxStatus failedStatus
    );
}
