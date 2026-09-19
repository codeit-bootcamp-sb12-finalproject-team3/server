package com.moduplaylist.batch.job.userplaylistprofileembedding.tasklet;

import com.moduplaylist.batch.job.userplaylistprofileembedding.UserPlaylistProfileEmbeddingTargetService;
import com.moduplaylist.infrastructure.recommendation.embedding.UserPlaylistProfileEmbeddingService;
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
public class UserPlaylistProfileEmbeddingTasklet implements Tasklet {

    private final UserPlaylistProfileEmbeddingService embeddingService;
    private final UserPlaylistProfileEmbeddingTargetService targetService;

    @Override
    public RepeatStatus execute(
            StepContribution contribution,
            ChunkContext chunkContext
    ) {
        List<UUID> targetIds = targetService.findTargetUserIds();
        List<UUID> failedIds = new ArrayList<>();

        for (UUID userId : targetIds) {
            try {
                UserProfileEmbeddingResult result = embeddingService.embedAndIndex(userId);
                log.info(
                        "사용자 플레이리스트 선호 임베딩 저장 완료 - userId={}, dimensions={}",
                        result.getUserId(),
                        result.getDimensions()
                );
            } catch (RuntimeException exception) {
                failedIds.add(userId);
                log.error(
                        "사용자 플레이리스트 선호 임베딩 저장 실패 - userId={}",
                        userId,
                        exception
                );
            }
        }

        log.info(
                "사용자 플레이리스트 선호 임베딩 배치 완료 - targets={}, succeeded={}, failed={}",
                targetIds.size(),
                targetIds.size() - failedIds.size(),
                failedIds.size()
        );
        if (!failedIds.isEmpty()) {
            throw new IllegalStateException(
                    "일부 사용자 플레이리스트 선호 임베딩 처리에 실패했습니다. failedUserIds="
                            + failedIds
            );
        }
        return RepeatStatus.FINISHED;
    }
}
