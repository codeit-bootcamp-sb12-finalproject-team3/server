package com.moduplaylist.api.content.service;

import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.infrastructure.opensearch.content.ContentIndexSynchronizer;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "mopl.opensearch",
    name = {"enabled", "content-initial-indexing-enabled"},
    havingValue = "true"
)
public class ContentIndexInitialIndexer {

    private static final int PAGE_SIZE = 100;
    private static final String ITEM_METRIC = "mopl.content.initial.index.items";

    private final ContentRepository contentRepository;
    private final ContentIndexSynchronizer contentIndexSynchronizer;
    private final MeterRegistry meterRegistry;

    @EventListener(ApplicationReadyEvent.class)
    public void index() {
        Set<UUID> failedIds = indexAllContents();
        incrementMetric("final_failed", failedIds.size());
        if (!failedIds.isEmpty()) {
            log.error("콘텐츠 초기 색인 최종 실패 - failedCount={}, contentIds={}",
                    failedIds.size(), failedIds);
        }
    }

    private Set<UUID> indexAllContents() {
        Set<UUID> failedIds = new LinkedHashSet<>();
        int pageNumber = 0;
        Page<Content> page;
        do {
            page = contentRepository.findAll(PageRequest.of(
                    pageNumber++, PAGE_SIZE, Sort.by(Sort.Direction.ASC, "id")));
            page.getContent().stream()
                    .map(Content::getId)
                    .filter(contentId -> !synchronize(contentId))
                    .forEach(failedIds::add);
        } while (page.hasNext());
        return failedIds;
    }

    private boolean synchronize(UUID contentId) {
        try {
            contentIndexSynchronizer.synchronize(contentId);
            return true;
        } catch (RuntimeException exception) {
            log.warn("콘텐츠 초기 색인에 최종 실패했습니다. contentId={}",
                    contentId, exception);
            return false;
        }
    }

    private void incrementMetric(String result, long count) {
        meterRegistry.counter(ITEM_METRIC, "result", result).increment(count);
    }
}
