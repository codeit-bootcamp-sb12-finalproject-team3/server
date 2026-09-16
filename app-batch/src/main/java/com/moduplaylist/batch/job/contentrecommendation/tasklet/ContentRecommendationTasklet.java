package com.moduplaylist.batch.job.contentrecommendation.tasklet;

import com.moduplaylist.infrastructure.recommendation.ContentRecommendationService;
import com.moduplaylist.batch.job.contentrecommendation.ContentRecommendationTargetService;
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
public class ContentRecommendationTasklet implements Tasklet {

    private final ContentRecommendationTargetService targetService;
    private final ContentRecommendationService recommendationService;

    @Override
    public RepeatStatus execute(
            StepContribution contribution,
            ChunkContext chunkContext
    ) {
        List<UUID> targetUserIds = targetService.findTargetUserIds();
        List<UUID> failedUserIds = new ArrayList<>();

// TODO: 현재는 Redis TTL 갱신과 신규 콘텐츠 반영을 위해 전체 사용자 추천을 재생성한다.
// 변경 없는 사용자까지 다시 계산하므로 사용자 수가 커질수록 OpenSearch 조회와 Redis write 비용이 증가하는 단점이 있다.
// 다만 신규 콘텐츠/콘텐츠 임베딩 변경도 추천 결과에 영향을 주고, Redis TTL 만료 및 배치 누락을 보정할 수 있어 현재 규모에서는 전체 재생성을 유지한다.
// 추후에는 변경 사용자만 증분 갱신하고, 전체 재생성은 저빈도 보정 배치로 분리하는 방향으로 개선할 수 있다.
        for (UUID userId : targetUserIds) {
            try {
                List<UUID> recommendationIds = recommendationService.generateAndCache(userId);
                log.info(
                        "콘텐츠 추천 캐시 저장 완료 - userId={}, recommendations={}",
                        userId,
                        recommendationIds.size()
                );
            } catch (RuntimeException exception) {
                failedUserIds.add(userId);
                log.error("콘텐츠 추천 생성 실패 - userId={}", userId, exception);
            }
        }

        log.info(
                "콘텐츠 추천 배치 완료 - targets={}, succeeded={}, failed={}",
                targetUserIds.size(),
                targetUserIds.size() - failedUserIds.size(),
                failedUserIds.size()
        );
        if (!failedUserIds.isEmpty()) {
            throw new IllegalStateException(
                    "일부 콘텐츠 추천 생성에 실패했습니다. failedUserIds=" + failedUserIds
            );
        }
        return RepeatStatus.FINISHED;
    }
}
