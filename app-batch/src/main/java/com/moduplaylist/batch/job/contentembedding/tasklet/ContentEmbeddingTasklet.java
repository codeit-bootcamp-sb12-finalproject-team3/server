package com.moduplaylist.batch.job.contentembedding.tasklet;

import com.moduplaylist.batch.job.contentembedding.dto.ContentEmbeddingResult;
import com.moduplaylist.batch.job.contentembedding.ContentEmbeddingService;
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
public class ContentEmbeddingTasklet implements Tasklet {

    private final ContentEmbeddingService embeddingService;

    @Value("#{jobParameters['contentId']}")
    private String contentId;

    @Override
    public RepeatStatus execute(
            StepContribution contribution,
            ChunkContext chunkContext
    ) {

        if (contentId == null || contentId.isBlank()) {
            throw new IllegalArgumentException(
                    "contentId JobParameter는 필수입니다."
            );
        }

        ContentEmbeddingResult result =
                embeddingService.embedAndIndex(UUID.fromString(contentId));

        log.info(
                "콘텐츠 임베딩 저장 완료 - contentId={}, dimensions={}",
                result.getContentId(),
                result.getDimensions()
        );

        return RepeatStatus.FINISHED;
    }
}