package com.moduplaylist.batch.job.contentembedding.tasklet;

import com.moduplaylist.batch.job.contentembedding.ContentEmbeddingService;
import com.moduplaylist.batch.job.contentembedding.ContentEmbeddingTargetService;
import com.moduplaylist.batch.job.contentembedding.ContentEmbeddingRunWindow;
import com.moduplaylist.batch.job.contentembedding.ContentEmbeddingRunWindowService;
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
    private final ContentEmbeddingRunWindowService runWindowService;

    @Override
    public RepeatStatus execute(
            StepContribution contribution,
            ChunkContext chunkContext
    ) {
// TODO 대규모 데이터 처리 최적화 -> 트러블슈팅으로 쓸 수 있을까하여 주석 달아둡니다
// 현재 전체 임베딩 대상을 List로 메모리에 적재하고 건별로 임베딩 생성/색인을 수행한다.
// 데이터 증가 시 메모리 사용량과 OpenAI/OpenSearch I/O 횟수가 증가할 수 있으므로,
// Paging/Chunk 기반 조회 + 임베딩 Batch 요청 + OpenSearch Bulk 색인 방식으로 개선한다.
        ContentEmbeddingRunWindow window = runWindowService.forExecution(
                chunkContext.getStepContext().getStepExecution().getJobExecution()
        );
        List<UUID> targetIds = targetService.findTargetContentIds(window);
        List<UUID> failedIds = new ArrayList<>();
        int deferred = 0;

        for (UUID contentId : targetIds) {
            try {
                ContentEmbeddingResult result = embeddingService.embedAndIndex(contentId);
                if (!result.isPublished()) {
                    deferred++;
                    log.info("콘텐츠 임베딩 게시 보류 - contentId={}, reason=SOURCE_CHANGED_OR_HIDDEN", contentId);
                    continue;
                }
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
                "콘텐츠 임베딩 배치 완료 - targets={}, succeeded={}, failed={}, deferred={}",
                targetIds.size(),
                targetIds.size() - failedIds.size() - deferred,
                failedIds.size(),
                deferred
        );

        if (!failedIds.isEmpty()) {
            throw new IllegalStateException(
                    "일부 콘텐츠 임베딩 저장에 실패했습니다. contentIds=" + failedIds
            );
        }

        return RepeatStatus.FINISHED;
    }
}
