package com.moduplaylist.batch.job.userprofileembedding.tasklet;

import com.moduplaylist.infrastructure.recommendation.embedding.dto.UserProfileEmbeddingResult;
import com.moduplaylist.infrastructure.recommendation.embedding.UserProfileEmbeddingService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class UserProfileEmbeddingTasklet implements Tasklet {

    private final UserProfileEmbeddingService embeddingService;

    @Value("#{jobParameters['userId']}")
    private String userId;

    @Override
    public RepeatStatus execute(
            StepContribution contribution,
            ChunkContext chunkContext
    ) {

        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException(
                    "userId JobParameter는 필수입니다."
            );
        }

        UserProfileEmbeddingResult result =
                embeddingService.embedAndIndex(UUID.fromString(userId));

        log.info(
                "사용자 프로필 임베딩 저장 완료 - userId={}, dimensions={}",
                result.getUserId(),
                result.getDimensions()
        );

        return RepeatStatus.FINISHED;
    }
}