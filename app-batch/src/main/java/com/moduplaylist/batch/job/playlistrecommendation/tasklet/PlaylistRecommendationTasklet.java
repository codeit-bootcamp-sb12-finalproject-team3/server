package com.moduplaylist.batch.job.playlistrecommendation.tasklet;

import com.moduplaylist.batch.job.playlistrecommendation.PlaylistRecommendationTargetService;
import com.moduplaylist.infrastructure.recommendation.PlaylistRecommendationService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class PlaylistRecommendationTasklet implements Tasklet {

    private final PlaylistRecommendationTargetService targetService;
    private final PlaylistRecommendationService recommendationService;

    @Override
    public RepeatStatus execute(
            StepContribution contribution,
            ChunkContext chunkContext
    ) {
        List<UUID> targetUserIds = targetService.findTargetUserIds();
        List<UUID> failedUserIds = new ArrayList<>();

        for (UUID userId : targetUserIds) {
            try {
                List<UUID> recommendationIds = recommendationService.generateAndCache(userId);
                log.info(
                        "플레이리스트 추천 캐시 저장 완료 - userId={}, recommendations={}",
                        userId,
                        recommendationIds.size()
                );
            } catch (RuntimeException exception) {
                failedUserIds.add(userId);
                log.error("플레이리스트 추천 생성 실패 - userId={}", userId, exception);
            }
        }

        log.info(
                "플레이리스트 추천 배치 완료 - targets={}, succeeded={}, failed={}",
                targetUserIds.size(),
                targetUserIds.size() - failedUserIds.size(),
                failedUserIds.size()
        );
        if (!failedUserIds.isEmpty()) {
            throw new IllegalStateException(
                    "일부 플레이리스트 추천 생성에 실패했습니다. failedUserIds=" + failedUserIds
            );
        }
        return RepeatStatus.FINISHED;
    }
}
