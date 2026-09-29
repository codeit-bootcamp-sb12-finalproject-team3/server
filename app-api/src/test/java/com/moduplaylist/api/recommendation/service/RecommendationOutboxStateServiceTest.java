package com.moduplaylist.api.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moduplaylist.core.recommendation.entity.RecommendationOutboxEvent;
import com.moduplaylist.core.recommendation.entity.RecommendationOutboxEventType;
import com.moduplaylist.core.recommendation.entity.RecommendationOutboxStatus;
import com.moduplaylist.core.recommendation.repository.RecommendationOutboxEventRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class RecommendationOutboxStateServiceTest {

    @Mock
    private RecommendationOutboxEventRepository outboxEventRepository;

    private RecommendationOutboxStateService service;

    @BeforeEach
    void setUp() {
        service = new RecommendationOutboxStateService(outboxEventRepository);
    }

    @Test
    void onlyOneWorkerClaimsEventWhenWorkersSeeSameCandidate() {
        UUID id = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-29T13:30:00Z");
        Duration staleAfter = Duration.ofMinutes(5);
        RecommendationOutboxEvent candidate = candidate(id, eventId, userId);
        when(outboxEventRepository.findClaimableEvents(
                eq(RecommendationOutboxStatus.PENDING),
                eq(RecommendationOutboxStatus.PROCESSING),
                eq(now),
                eq(now.minus(staleAfter)),
                any(Pageable.class)
        )).thenReturn(List.of(candidate));
        when(outboxEventRepository.tryClaim(
                eq(id),
                any(UUID.class),
                eq(now),
                eq(now.minus(staleAfter)),
                eq(RecommendationOutboxStatus.PENDING),
                eq(RecommendationOutboxStatus.PROCESSING)
        )).thenReturn(1, 0);

        List<RecommendationOutboxClaim> firstWorkerClaims =
                service.claimAvailable(1, now, staleAfter);
        List<RecommendationOutboxClaim> secondWorkerClaims =
                service.claimAvailable(1, now, staleAfter);

        assertThat(firstWorkerClaims).hasSize(1);
        assertThat(firstWorkerClaims.get(0).id()).isEqualTo(id);
        assertThat(firstWorkerClaims.get(0).eventId()).isEqualTo(eventId);
        assertThat(firstWorkerClaims.get(0).userId()).isEqualTo(userId);
        assertThat(firstWorkerClaims.get(0).eventType())
                .isEqualTo(RecommendationOutboxEventType.INITIAL_PREFERENCE_CREATED);
        assertThat(secondWorkerClaims).isEmpty();

        ArgumentCaptor<UUID> claimTokenCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(outboxEventRepository, times(2)).tryClaim(
                eq(id),
                claimTokenCaptor.capture(),
                eq(now),
                eq(now.minus(staleAfter)),
                eq(RecommendationOutboxStatus.PENDING),
                eq(RecommendationOutboxStatus.PROCESSING)
        );
        assertThat(claimTokenCaptor.getAllValues()).doesNotHaveDuplicates();
    }

    @Test
    void wrongClaimTokenCannotCompleteEvent() {
        UUID id = UUID.randomUUID();
        UUID wrongClaimToken = UUID.randomUUID();
        Instant completedAt = Instant.parse("2026-09-29T13:31:00Z");
        when(outboxEventRepository.complete(
                id,
                wrongClaimToken,
                completedAt,
                RecommendationOutboxStatus.PROCESSING,
                RecommendationOutboxStatus.COMPLETED
        )).thenReturn(0);

        boolean completed = service.complete(id, wrongClaimToken, completedAt);

        assertThat(completed).isFalse();
    }

    @Test
    void matchingClaimTokenCanScheduleRetry() {
        UUID id = UUID.randomUUID();
        UUID claimToken = UUID.randomUUID();
        Instant failedAt = Instant.parse("2026-09-29T13:32:00Z");
        Instant nextRetryAt = failedAt.plusSeconds(10);
        when(outboxEventRepository.retry(
                id,
                claimToken,
                nextRetryAt,
                "OpenSearch unavailable",
                failedAt,
                RecommendationOutboxStatus.PROCESSING,
                RecommendationOutboxStatus.PENDING
        )).thenReturn(1);

        boolean scheduled = service.retry(
                id,
                claimToken,
                failedAt,
                nextRetryAt,
                "OpenSearch unavailable"
        );

        assertThat(scheduled).isTrue();
    }

    @Test
    void matchingClaimTokenCanFailEvent() {
        UUID id = UUID.randomUUID();
        UUID claimToken = UUID.randomUUID();
        Instant failedAt = Instant.parse("2026-09-29T13:33:00Z");
        when(outboxEventRepository.fail(
                id,
                claimToken,
                "retry exhausted",
                failedAt,
                RecommendationOutboxStatus.PROCESSING,
                RecommendationOutboxStatus.FAILED
        )).thenReturn(1);

        boolean failed = service.fail(id, claimToken, failedAt, "retry exhausted");

        assertThat(failed).isTrue();
    }

    private RecommendationOutboxEvent candidate(UUID id, UUID eventId, UUID userId) {
        RecommendationOutboxEvent candidate = mock(RecommendationOutboxEvent.class);
        when(candidate.getId()).thenReturn(id);
        when(candidate.getEventId()).thenReturn(eventId);
        when(candidate.getEventType())
                .thenReturn(RecommendationOutboxEventType.INITIAL_PREFERENCE_CREATED);
        when(candidate.getUserId()).thenReturn(userId);
        return candidate;
    }
}
