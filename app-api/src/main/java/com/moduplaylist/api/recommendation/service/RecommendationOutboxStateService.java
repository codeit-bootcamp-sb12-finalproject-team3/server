package com.moduplaylist.api.recommendation.service;

import com.github.f4b6a3.uuid.UuidCreator;
import com.moduplaylist.core.recommendation.entity.RecommendationOutboxEvent;
import com.moduplaylist.core.recommendation.entity.RecommendationOutboxStatus;
import com.moduplaylist.core.recommendation.repository.RecommendationOutboxEventRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecommendationOutboxStateService {

    private final RecommendationOutboxEventRepository outboxEventRepository;

    @Transactional
    public List<RecommendationOutboxClaim> claimAvailable(
            int limit,
            Instant now,
            Duration staleAfter
    ) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        Objects.requireNonNull(now);
        if (Objects.requireNonNull(staleAfter).isNegative() || staleAfter.isZero()) {
            throw new IllegalArgumentException("staleAfter must be positive");
        }

        Instant staleBefore = now.minus(staleAfter);
        List<RecommendationOutboxEvent> candidates =
                outboxEventRepository.findClaimableEvents(
                        RecommendationOutboxStatus.PENDING,
                        RecommendationOutboxStatus.PROCESSING,
                        now,
                        staleBefore,
                        PageRequest.of(0, limit)
                );

        List<RecommendationOutboxClaim> claims = new ArrayList<>(candidates.size());
        for (RecommendationOutboxEvent candidate : candidates) {
            UUID claimToken = UuidCreator.getTimeOrderedEpoch();
            int claimed = outboxEventRepository.tryClaim(
                    candidate.getId(),
                    claimToken,
                    now,
                    staleBefore,
                    RecommendationOutboxStatus.PENDING,
                    RecommendationOutboxStatus.PROCESSING
            );
            if (claimed == 1) {
                claims.add(new RecommendationOutboxClaim(
                        candidate.getId(),
                        candidate.getEventId(),
                        candidate.getEventType(),
                        candidate.getUserId(),
                        candidate.getRetryCount(),
                        claimToken
                ));
            }
        }
        return List.copyOf(claims);
    }

    @Transactional
    public boolean complete(UUID id, UUID claimToken, Instant completedAt) {
        requireClaim(id, claimToken, completedAt);
        return outboxEventRepository.complete(
                id,
                claimToken,
                completedAt,
                RecommendationOutboxStatus.PROCESSING,
                RecommendationOutboxStatus.COMPLETED
        ) == 1;
    }

    @Transactional
    public boolean retry(
            UUID id,
            UUID claimToken,
            Instant failedAt,
            Instant nextRetryAt,
            String lastError
    ) {
        requireClaim(id, claimToken, failedAt);
        Objects.requireNonNull(nextRetryAt);
        Objects.requireNonNull(lastError);
        return outboxEventRepository.retry(
                id,
                claimToken,
                nextRetryAt,
                lastError,
                failedAt,
                RecommendationOutboxStatus.PROCESSING,
                RecommendationOutboxStatus.PENDING
        ) == 1;
    }

    @Transactional
    public boolean fail(
            UUID id,
            UUID claimToken,
            Instant failedAt,
            String lastError
    ) {
        requireClaim(id, claimToken, failedAt);
        Objects.requireNonNull(lastError);
        return outboxEventRepository.fail(
                id,
                claimToken,
                lastError,
                failedAt,
                RecommendationOutboxStatus.PROCESSING,
                RecommendationOutboxStatus.FAILED
        ) == 1;
    }

    private void requireClaim(UUID id, UUID claimToken, Instant changedAt) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(claimToken);
        Objects.requireNonNull(changedAt);
    }
}
