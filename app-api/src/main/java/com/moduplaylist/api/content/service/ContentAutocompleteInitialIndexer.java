package com.moduplaylist.api.content.service;

import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.repository.ContentRepository;
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

	private final ContentRepository contentRepository;
	private final ContentAutocompleteIndexService autocompleteIndexService;

	@EventListener(ApplicationReadyEvent.class)
	public void index() {
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
				.forEach(this::synchronize);
		} while (page.hasNext());
	}

	private void synchronize(UUID contentId) {
		try {
			autocompleteIndexService.synchronize(contentId);
		} catch (RuntimeException exception) {
			log.warn("자동완성 초기 동기화에 실패했습니다. contentId={}", contentId, exception);
		}
	}
}
