package com.moduplaylist.api.content.service;

import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
@RequiredArgsConstructor
@ConditionalOnProperty(
	prefix = "mopl.opensearch",
	name = "autocomplete-initial-indexing-enabled",
	havingValue = "true"
)
public class ContentAutocompleteDevIndexer {

	private final ContentRepository contentRepository;
	private final ContentAutocompleteIndexService autocompleteIndexService;

	@EventListener(ApplicationReadyEvent.class)
	public void index() {
		contentRepository.findAll().stream()
			.map(Content::getId)
			.forEach(autocompleteIndexService::synchronize);
	}
}
