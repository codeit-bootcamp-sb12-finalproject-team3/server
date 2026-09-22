package com.moduplaylist.batch.job.contentimport;

import com.moduplaylist.batch.job.contentembedding.ContentEmbeddingService;
import com.moduplaylist.infrastructure.opensearch.content.ContentAutocompleteSynchronizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class SportSearchSyncListener {
    private final ContentEmbeddingService contentEmbeddingService;
    private final ContentAutocompleteSynchronizer autocompleteSynchronizer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void synchronize(SportSearchSyncRequested event) {
        try {
            contentEmbeddingService.indexSportSearchDocument(event.contentId());
        } catch (RuntimeException exception) {
            log.warn("스포츠 콘텐츠 검색 문서 동기화에 실패했습니다. contentId={}",
                event.contentId(), exception);
        }
        try {
            autocompleteSynchronizer.synchronize(event.contentId());
        } catch (RuntimeException exception) {
            log.warn("스포츠 콘텐츠 자동완성 동기화에 실패했습니다. contentId={}",
                event.contentId(), exception);
        }
    }

}
