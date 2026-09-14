package com.moduplaylist.batch.job.contentembedding.tasklet;

import com.moduplaylist.batch.job.contentembedding.ContentEmbeddingService;
import com.moduplaylist.batch.job.contentembedding.ContentEmbeddingTargetService;
import com.moduplaylist.batch.job.contentembedding.dto.ContentEmbeddingResult;
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
public class ContentEmbeddingTasklet implements Tasklet {

    private final ContentEmbeddingService embeddingService;
    private final ContentEmbeddingTargetService targetService;

    @Override
    public RepeatStatus execute(
            StepContribution contribution,
            ChunkContext chunkContext
    ) {
// TODO 대규모 데이터 처리 최적화 -> 트러블슈팅으로 쓸 수 있을까하여 주석 달아둡니다
// 현재 전체 콘텐츠를 메모리에 조회한 뒤 임베딩 대상을 판별하고 순차 처리한다.
// 데이터 증가 시 메모리 사용량 및 OpenSearch 조회 비용이 증가할 수 있으므로,
// 변경 대상 페이징 조회 + Chunk 기반 처리 방식으로 개선한다.
        List<UUID> targetIds = targetService.findTargetContentIds();
        List<UUID> failedIds = new ArrayList<>();

        for (UUID contentId : targetIds) {
            try {
                ContentEmbeddingResult result = embeddingService.embedAndIndex(contentId);
                log.info(
                        "콘텐츠 임베딩 저장 완료 - contentId={}, dimensions={}",
                        result.getContentId(),
                        result.getDimensions()
                );
            } catch (RuntimeException exception) {
                failedIds.add(contentId);
                log.error("콘텐츠 임베딩 저장 실패 - contentId={}", contentId, exception);
            }
        }

        log.info(
                "콘텐츠 임베딩 배치 완료 - targets={}, succeeded={}, failed={}",
                targetIds.size(),
                targetIds.size() - failedIds.size(),
                failedIds.size()
        );

        if (!failedIds.isEmpty()) {
            throw new IllegalStateException(
                    "일부 콘텐츠 임베딩 처리에 실패했습니다. failedContentIds=" + failedIds
            );
        }

        return RepeatStatus.FINISHED;
    }
}
