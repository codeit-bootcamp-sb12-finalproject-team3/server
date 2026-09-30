package com.moduplaylist.api.recommendation.service;

import com.moduplaylist.api.recommendation.metric.InitialPreferencePostProcessingMetrics;
import com.moduplaylist.infrastructure.recommendation.ContentRecommendationService;
import com.moduplaylist.infrastructure.recommendation.PlaylistRecommendationService;
import com.moduplaylist.infrastructure.recommendation.embedding.UserContentProfileEmbeddingService;
import com.moduplaylist.infrastructure.recommendation.embedding.UserPlaylistProfileEmbeddingService;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class InitialPreferencePostProcessingService {

    private final UserContentProfileEmbeddingService userContentProfileEmbeddingService;
    private final UserPlaylistProfileEmbeddingService userPlaylistProfileEmbeddingService;
    private final ContentRecommendationService contentRecommendationService;
    private final PlaylistRecommendationService playlistRecommendationService;
    private final InitialPreferencePostProcessingMetrics metrics;

    public void process(UUID eventId, UUID userId) {
        long startedAt = System.nanoTime();
        log.info(
                "초기 선호 추천 후처리를 시작합니다. eventId={}, userId={}",
                eventId,
                userId
        );
        String stage = "content_profile_embedding";
        int completedStageCount = 0;
        try {
            userContentProfileEmbeddingService.embedAndIndex(userId);
            completedStageCount++;
            stage = "playlist_profile_embedding";
            userPlaylistProfileEmbeddingService.embedAndIndex(userId);
            completedStageCount++;
            stage = "content_recommendation";
            contentRecommendationService.generateAndCache(userId);
            completedStageCount++;
            stage = "playlist_recommendation";
            playlistRecommendationService.generateAndCache(userId);
            completedStageCount++;
            metrics.recordSuccess();
        } catch (RuntimeException exception) {
            Duration duration = elapsedSince(startedAt);
            metrics.recordFailure(completedStageCount > 0);
            log.error(
                    "초기 선호 추천 후처리에 실패했습니다. "
                            + "stage={}, eventId={}, userId={}, durationMs={}",
                    stage,
                    eventId,
                    userId,
                    duration.toMillis(),
                    exception
            );
            throw exception;
        } finally {
            metrics.recordDuration(elapsedSince(startedAt));
        }
        Duration duration = elapsedSince(startedAt);
        log.info(
                "초기 선호 추천 후처리를 완료했습니다. eventId={}, userId={}, durationMs={}",
                eventId,
                userId,
                duration.toMillis()
        );
    }

    private Duration elapsedSince(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt);
    }
}
