package com.moduplaylist.batch.job.playlistembedding.tasklet;

import com.moduplaylist.batch.job.playlistembedding.PlaylistEmbeddingService;
import com.moduplaylist.batch.job.playlistembedding.PlaylistEmbeddingTargetService;
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
public class PlaylistEmbeddingTasklet implements Tasklet {

    private final PlaylistEmbeddingService embeddingService;
    private final PlaylistEmbeddingTargetService targetService;

    @Override
    public RepeatStatus execute(
            StepContribution contribution,
            ChunkContext chunkContext
    ) {
        List<UUID> deletedPlaylistIds = targetService.findDeletedPlaylistIds();
        List<UUID> deletionFailedIds = new ArrayList<>();
        for (UUID playlistId : deletedPlaylistIds) {
            try {
                embeddingService.deleteFromIndex(playlistId);
            } catch (RuntimeException exception) {
                deletionFailedIds.add(playlistId);
                log.error("삭제된 플레이리스트 벡터 정리 실패 - playlistId={}", playlistId, exception);
            }
        }

        List<UUID> targetIds = targetService.findTargetPlaylistIds();
        List<UUID> failedIds = new ArrayList<>();
        for (UUID playlistId : targetIds) {
            try {
                embeddingService.embedAndIndex(playlistId);
                log.info("플레이리스트 임베딩 저장 완료 - playlistId={}", playlistId);
            } catch (RuntimeException exception) {
                failedIds.add(playlistId);
                log.error("플레이리스트 임베딩 저장 실패 - playlistId={}", playlistId, exception);
            }
        }

        log.info(
                "플레이리스트 임베딩 배치 완료 - targets={}, succeeded={}, failed={}, "
                        + "deletedVectors={}, deletionFailed={}",
                targetIds.size(),
                targetIds.size() - failedIds.size(),
                failedIds.size(),
                deletedPlaylistIds.size(),
                deletionFailedIds.size()
        );
        if (!deletionFailedIds.isEmpty() || !failedIds.isEmpty()) {
            throw new IllegalStateException(
                    "일부 플레이리스트 임베딩 처리에 실패했습니다. failedPlaylistIds="
                            + failedIds
                            + ", deletionFailedPlaylistIds="
                            + deletionFailedIds
            );
        }
        return RepeatStatus.FINISHED;
    }
}
