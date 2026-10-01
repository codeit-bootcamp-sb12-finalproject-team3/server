package com.moduplaylist.api.recommendation.outbox;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moduplaylist.api.recommendation.service.InitialPreferencePostProcessingService;
import com.moduplaylist.api.recommendation.service.RecommendationOutboxClaim;
import com.moduplaylist.api.recommendation.service.RecommendationOutboxStateService;
import com.moduplaylist.core.recommendation.entity.RecommendationOutboxEventType;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class RecommendationOutboxWorkerTest {

    private static final Instant NOW = Instant.parse("2026-09-29T14:00:00Z");

    @Mock
    private RecommendationOutboxStateService stateService;
    @Mock
    private InitialPreferencePostProcessingService postProcessingService;

    private RecommendationOutboxWorker worker;

    @BeforeEach
    void setUp() {
        RecommendationOutboxProperties properties = new RecommendationOutboxProperties();
        properties.setClaimLimit(10);
        properties.setProcessingTimeout(Duration.ofMinutes(10));
        worker = new RecommendationOutboxWorker(
                stateService,
                postProcessingService,
                new RecommendationOutboxRetryPolicy(),
                properties,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void processesClaimOutsideTransactionAndCompletesWithClaimToken() {
        RecommendationOutboxClaim claim = claim();
        when(stateService.claimAvailable(10, NOW, Duration.ofMinutes(10)))
                .thenReturn(List.of(claim));
        when(stateService.complete(claim.id(), claim.claimToken(), NOW)).thenReturn(true);
        AtomicBoolean transactionActive = new AtomicBoolean(true);
        org.mockito.Mockito.doAnswer(invocation -> {
            transactionActive.set(
                    TransactionSynchronizationManager.isActualTransactionActive()
            );
            return null;
        }).when(postProcessingService).process(claim.eventId(), claim.userId());

        worker.poll();

        InOrder order = inOrder(postProcessingService, stateService);
        order.verify(postProcessingService).process(claim.eventId(), claim.userId());
        order.verify(stateService).complete(claim.id(), claim.claimToken(), NOW);
        org.assertj.core.api.Assertions.assertThat(transactionActive).isFalse();
    }

    @Test
    void schedulesFirstRetryTenSecondsAfterFailure() {
        RecommendationOutboxClaim claim = claim();
        when(stateService.claimAvailable(10, NOW, Duration.ofMinutes(10)))
                .thenReturn(List.of(claim));
        org.mockito.Mockito.doThrow(new IllegalStateException("OpenSearch unavailable"))
                .when(postProcessingService)
                .process(claim.eventId(), claim.userId());
        when(stateService.retry(
                claim.id(),
                claim.claimToken(),
                NOW,
                NOW.plusSeconds(10),
                "IllegalStateException: OpenSearch unavailable"
        )).thenReturn(true);

        worker.poll();

        verify(stateService).retry(
                claim.id(),
                claim.claimToken(),
                NOW,
                NOW.plusSeconds(10),
                "IllegalStateException: OpenSearch unavailable"
        );
        verify(stateService, never()).complete(
                claim.id(),
                claim.claimToken(),
                NOW
        );
        verify(stateService, never()).fail(
                claim.id(),
                claim.claimToken(),
                NOW,
                "IllegalStateException: OpenSearch unavailable"
        );
    }

    @Test
    void marksEventFailedWhenRetriesAreExhausted() {
        RecommendationOutboxClaim claim = claim(3);
        when(stateService.claimAvailable(10, NOW, Duration.ofMinutes(10)))
                .thenReturn(List.of(claim));
        org.mockito.Mockito.doThrow(new IllegalStateException("OpenSearch unavailable"))
                .when(postProcessingService)
                .process(claim.eventId(), claim.userId());
        when(stateService.fail(
                claim.id(),
                claim.claimToken(),
                NOW,
                "IllegalStateException: OpenSearch unavailable"
        )).thenReturn(true);

        worker.poll();

        verify(stateService).fail(
                claim.id(),
                claim.claimToken(),
                NOW,
                "IllegalStateException: OpenSearch unavailable"
        );
        verify(stateService, never()).retry(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    private RecommendationOutboxClaim claim() {
        return claim(0);
    }

    private RecommendationOutboxClaim claim(int retryCount) {
        return new RecommendationOutboxClaim(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                RecommendationOutboxEventType.INITIAL_PREFERENCE_CREATED,
                UUID.fromString("00000000-0000-0000-0000-000000000003"),
                retryCount,
                UUID.fromString("00000000-0000-0000-0000-000000000004")
        );
    }
}
