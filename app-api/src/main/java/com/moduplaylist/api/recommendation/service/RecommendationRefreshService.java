package com.moduplaylist.api.recommendation.service;

import com.moduplaylist.infrastructure.recommendation.ContentRecommendationService;
import com.moduplaylist.infrastructure.recommendation.RecommendationRefreshProperties;
import com.moduplaylist.infrastructure.recommendation.embedding.UserContentProfileEmbeddingService;
import com.moduplaylist.infrastructure.redis.recommendation.RecommendationRefreshRedisRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mopl.embedding", name = "enabled", havingValue = "true")
public class RecommendationRefreshService {

    private final RecommendationRefreshRedisRepository refreshRedisRepository;
    private final RecommendationRefreshProperties properties;
    private final UserContentProfileEmbeddingService profileEmbeddingService;
    private final ContentRecommendationService recommendationService;

    public void recordActivity(UUID userId, double appliedDelta) {
        double amount = Math.abs(appliedDelta);
        if (amount == 0.0) {
            return;
        }

        double accumulated = refreshRedisRepository.addActivity(userId, amount);
        if (accumulated >= properties.getThreshold()) {
            refreshWhileEligible(userId);
        }
    }

    private void refreshWhileEligible(UUID userId) {
        boolean checkAgain;
        do {
            checkAgain = refreshOnce(userId);
        } while (checkAgain
                && refreshRedisRepository.getActivity(userId) >= properties.getThreshold());
    }

    private boolean refreshOnce(UUID userId) {
        String token = UUID.randomUUID().toString();
        if (!refreshRedisRepository.tryLock(userId, token, properties.getLockTtl())) {
            return false;
        }

        try {
            double claimed = refreshRedisRepository.claimActivity(userId);
            if (claimed < properties.getThreshold()) {
                refreshRedisRepository.restoreActivity(userId, claimed);
                return false;
            }

            try {
                profileEmbeddingService.embedAndIndex(userId);
                recommendationService.generateAndCache(userId);
                return true;
            } catch (RuntimeException exception) {
                refreshRedisRepository.restoreActivity(userId, claimed);
                throw exception;
            }
        } finally {
            if (!refreshRedisRepository.unlock(userId, token)) {
                log.warn("추천 갱신 lock을 해제하지 못했습니다. userId={}", userId);
            }
        }
    }
}
