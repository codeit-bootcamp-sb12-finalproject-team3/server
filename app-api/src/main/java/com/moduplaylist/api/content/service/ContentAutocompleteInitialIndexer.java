package com.moduplaylist.api.content.service;

import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.repository.ContentRepository;
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
	name = {"enabled", "autocomplete-initial-indexing-enabled"},
	havingValue = "true"
)
public class ContentAutocompleteInitialIndexer {

	private static final int PAGE_SIZE = 100;
	private static final int MAX_RETRY_ATTEMPTS = 2;
	private static final String ITEM_METRIC = "mopl.content.autocomplete.initial.index.items";

	private final ContentRepository contentRepository;
	private final ContentAutocompleteIndexService autocompleteIndexService;
	private final MeterRegistry meterRegistry;

	@EventListener(ApplicationReadyEvent.class)
	public void index() {
		Set<UUID> failedIds = indexAllContents();
		incrementMetric("initial_failed", failedIds.size());

		for (int retryAttempt = 1;
			 retryAttempt <= MAX_RETRY_ATTEMPTS && !failedIds.isEmpty();
			 retryAttempt++) {
			int retryTargetCount = failedIds.size();
			Set<UUID> remainingFailedIds = retry(failedIds, retryAttempt);
			int recoveredCount = retryTargetCount - remainingFailedIds.size();
			incrementMetric("retry_succeeded", recoveredCount);
			log.info(
				"자동완성 초기 인덱싱 재시도 완료 - retryAttempt={}, targets={}, succeeded={}, failed={}",
				retryAttempt,
				retryTargetCount,
				recoveredCount,
				remainingFailedIds.size()
			);
			failedIds = remainingFailedIds;
		}

		incrementMetric("final_failed", failedIds.size());
		if (!failedIds.isEmpty()) {
			log.error(
				"자동완성 초기 인덱싱 최종 실패 - failedCount={}, contentIds={}",
				failedIds.size(),
				failedIds
			);
		}
	}

	private Set<UUID> indexAllContents() {
		Set<UUID> failedIds = new LinkedHashSet<>();
		int pageNumber = 0;
		Page<Content> page;
		do {
			page = contentRepository.findAll(PageRequest.of(
				pageNumber++,
				PAGE_SIZE,
				Sort.by(Sort.Direction.ASC, "id")
			));
			page.getContent().stream()
				.map(Content::getId)
				.filter(contentId -> !synchronize(contentId, 1))
				.forEach(failedIds::add);
		} while (page.hasNext());
		return failedIds;
	}

	private Set<UUID> retry(Set<UUID> contentIds, int retryAttempt) {
		Set<UUID> failedIds = new LinkedHashSet<>();
		for (UUID contentId : contentIds) {
			if (!synchronize(contentId, retryAttempt + 1)) {
				failedIds.add(contentId);
			}
		}
		return failedIds;
	}

	private boolean synchronize(UUID contentId, int attempt) {
		try {
			autocompleteIndexService.synchronize(contentId);
			return true;
		} catch (RuntimeException exception) {
			log.warn(
				"자동완성 초기 동기화에 실패했습니다. contentId={}, attempt={}",
				contentId,
				attempt,
				exception
			);
			return false;
		}
	}

	private void incrementMetric(String result, long count) {
		meterRegistry.counter(ITEM_METRIC, "result", result).increment(count);
	}
}
