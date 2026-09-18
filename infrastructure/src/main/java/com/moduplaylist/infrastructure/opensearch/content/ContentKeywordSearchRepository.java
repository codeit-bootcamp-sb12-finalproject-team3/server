package com.moduplaylist.infrastructure.opensearch.content;

import com.moduplaylist.core.content.exception.ContentSearchUnavailableException;
import com.moduplaylist.infrastructure.opensearch.config.OpenSearchProperties;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mopl.opensearch", name = "enabled", havingValue = "true")
public class ContentKeywordSearchRepository {

	private static final int MAX_RESULTS = 100;
	private static final String TV_SERIES_TYPE = "tvSeries";

	private final OpenSearchClient openSearchClient;
	private final OpenSearchProperties properties;

	public List<UUID> findContentIds(String keyword, String contentType) {
		Query query = Query.of(q -> q.bool(bool -> {
			bool.must(must -> must.multiMatch(multiMatch -> multiMatch
				.query(keyword)
				.fields("title^3", "description", "genres", "tags")));
			if (contentType != null) {
				bool.filter(filter -> filter.term(term -> term
					.field("type")
					.value(FieldValue.of(contentType))));
			} else {
				bool.mustNot(mustNot -> mustNot.term(term -> term
					.field("type")
					.value(FieldValue.of(TV_SERIES_TYPE))));
			}
			return bool;
		}));

		try {
			return openSearchClient.search(request -> request
					.index(properties.getContentIndex())
					.size(MAX_RESULTS)
					.source(source -> source.fetch(false))
					.query(query),
				Void.class)
				.hits().hits().stream()
				.map(hit -> UUID.fromString(hit.id()))
				.toList();
		} catch (IOException | IllegalArgumentException exception) {
			throw new ContentSearchUnavailableException(exception);
		}
	}
}
