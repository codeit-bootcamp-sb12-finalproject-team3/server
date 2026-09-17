package com.moduplaylist.batch.job.userprofileembedding.tasklet;

import com.moduplaylist.batch.job.userprofileembedding.UserProfileEmbeddingTargetService;
import com.moduplaylist.batch.job.userprofileembedding.UserProfileRecommendationCleanupService;
import com.moduplaylist.infrastructure.recommendation.embedding.UserProfileEmbeddingService;
import com.moduplaylist.infrastructure.recommendation.embedding.dto.UserProfileEmbeddingResult;
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
public class UserProfileEmbeddingTasklet implements Tasklet {

    private final UserProfileEmbeddingService embeddingService;
    private final UserProfileEmbeddingTargetService targetService;
    private final UserProfileRecommendationCleanupService cleanupService;

    @Override
    public RepeatStatus execute(
            StepContribution contribution,
            ChunkContext chunkContext
    ) {
// TODO 대규모 데이터 처리 최적화 -> 트러블슈팅으로 쓸 수 있을까하여 주석 달아둡니다
// 현재 전체 임베딩 대상을 List로 메모리에 적재하고 건별로 임베딩 생성/색인을 수행한다.
// 데이터 증가 시 메모리 사용량과 OpenAI/OpenSearch I/O 횟수가 증가할 수 있으므로,
// Paging/Chunk 기반 조회 + 임베딩 Batch 요청 + OpenSearch Bulk 색인 방식으로 개선한다.
        List<UUID> cleanupTargetIds = targetService.findUserIdsWithoutPositivePreference();
        List<UUID> cleanupFailedIds = new ArrayList<>();
        for (UUID userId : cleanupTargetIds) {
            try {
                cleanupService.removeStaleRecommendation(userId);
            } catch (RuntimeException exception) {
                cleanupFailedIds.add(userId);
                log.error("비활성 사용자 추천 데이터 삭제 실패 - userId={}", userId, exception);
            }
        }

        List<UUID> targetIds = targetService.findTargetUserIds();
        List<UUID> embeddingFailedIds = new ArrayList<>();

        for (UUID userId : targetIds) {
            try {
                UserProfileEmbeddingResult result = embeddingService.embedAndIndex(userId);
                log.info(
                        "사용자 프로필 임베딩 저장 완료 - userId={}, dimensions={}",
                        result.getUserId(),
                        result.getDimensions()
                );
            } catch (RuntimeException exception) {
                embeddingFailedIds.add(userId);
                log.error("사용자 프로필 임베딩 저장 실패 - userId={}", userId, exception);
            }
        }

        log.info(
                "사용자 프로필 임베딩 배치 완료 - targets={}, succeeded={}, failed={}, "
                        + "cleanupTargets={}, cleanupFailed={}",
                targetIds.size(),
                targetIds.size() - embeddingFailedIds.size(),
                embeddingFailedIds.size(),
                cleanupTargetIds.size(),
                cleanupFailedIds.size()
        );

        if (!cleanupFailedIds.isEmpty() || !embeddingFailedIds.isEmpty()) {
            throw new IllegalStateException(
                    "일부 사용자 프로필 처리에 실패했습니다. embeddingFailedUserIds="
                            + embeddingFailedIds
                            + ", cleanupFailedUserIds="
                            + cleanupFailedIds
            );
        }

        return RepeatStatus.FINISHED;
    }
}
