package com.moduplaylist.infrastructure.opensearch.content;

import com.moduplaylist.core.content.exception.ContentSearchUnavailableException;
import com.moduplaylist.infrastructure.opensearch.config.OpenSearchProperties;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mopl.opensearch", name = "enabled", havingValue = "true")
public class ContentAutocompleteIndexRepository {

	private final OpenSearchClient openSearchClient;
	private final OpenSearchProperties properties;

	public void upsert(ContentAutocompleteDocument document) {
		try {
			openSearchClient.update(request -> request
				.index(properties.getContentAutocompleteIndex())
				.id(document.getContentId().toString())
				.doc(document)
				.docAsUpsert(true)
				.detectNoop(true), ContentAutocompleteDocument.class);
		} catch (IOException exception) {
			throw new ContentSearchUnavailableException(exception);
		}
	}

	public void deleteById(UUID contentId) {
		try {
			openSearchClient.delete(request -> request
				.index(properties.getContentAutocompleteIndex())
				.id(contentId.toString()));
		} catch (IOException exception) {
			throw new ContentSearchUnavailableException(exception);
		}
	}
}
