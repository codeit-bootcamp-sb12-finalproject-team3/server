package com.moduplaylist.api.content.service;

import com.moduplaylist.infrastructure.opensearch.content.ContentAutocompleteSynchronizer;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContentAutocompleteIndexService {

	private final ContentAutocompleteSynchronizer synchronizer;

	public void synchronize(UUID contentId) {
		synchronizer.synchronize(contentId);
	}
}
