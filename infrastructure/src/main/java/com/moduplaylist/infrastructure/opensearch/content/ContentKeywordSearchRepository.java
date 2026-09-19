package com.moduplaylist.infrastructure.opensearch.content;

import com.moduplaylist.core.content.exception.ContentSearchUnavailableException;
import com.moduplaylist.infrastructure.opensearch.config.OpenSearchProperties;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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
	private static final int LIKED_ID_BATCH_SIZE = 1_000;
	private static final String TV_SERIES_TYPE = "tvSeries";

	private final OpenSearchClient openSearchClient;
	private final OpenSearchProperties properties;

	public List<UUID> findContentIds(String keyword, String contentType) {
		return search(keyword, contentType, null, MAX_RESULTS);
	}

	public List<UUID> findAutocompleteIds(String query, int limit) {
		return search(query, null, null, limit);
	}

	public List<UUID> findContentIdsWithin(
		String keyword,
		String contentType,
		List<UUID> candidateIds
	) {
		if (candidateIds.isEmpty()) {
			return List.of();
		}

		List<UUID> matchedIds = new ArrayList<>();
		for (int fromIndex = 0; fromIndex < candidateIds.size(); fromIndex += LIKED_ID_BATCH_SIZE) {
			int toIndex = Math.min(fromIndex + LIKED_ID_BATCH_SIZE, candidateIds.size());
			List<UUID> idBatch = candidateIds.subList(fromIndex, toIndex);
			matchedIds.addAll(search(keyword, contentType, idBatch, idBatch.size()));
		}
		return List.copyOf(matchedIds);
	}

	private List<UUID> search(
		String keyword,
		String contentType,
		Collection<UUID> candidateIds,
		int maxResults
	) {
		Query query = Query.of(q -> q.bool(bool -> {
			bool.must(must -> must.multiMatch(multiMatch -> multiMatch
				.query(keyword)
				.fields(
					"title^3",
					"description",
					"genres",
					"tags",
					"sportType",
					"leagueName",
					"homeTeamName",
					"awayTeamName",
					"venue"
				)));
			bool.mustNot(mustNot -> mustNot.term(term -> term
				.field("hidden")
				.value(FieldValue.of(true))));
			if (candidateIds != null) {
				bool.filter(filter -> filter.terms(terms -> terms
					.field("_id")
					.terms(values -> values.value(candidateIds.stream()
						.map(id -> FieldValue.of(id.toString()))
						.toList()))));
			}
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
					.size(maxResults)
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
