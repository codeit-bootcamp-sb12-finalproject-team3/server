package com.moduplaylist.infrastructure.opensearch.content;

import com.moduplaylist.core.content.exception.ContentSearchUnavailableException;
import com.moduplaylist.infrastructure.opensearch.config.OpenSearchProperties;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.OpenSearchException;
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
		return search(keyword, contentType);
	}

	public List<ContentAutocompleteCandidate> findAutocompleteCandidates(String query, int limit) {
		String normalizedQuery = query.toLowerCase(Locale.ROOT);
		Query exactMatch = Query.of(q -> q.term(term -> term
			.field("suggestions.text.normalized")
			.value(FieldValue.of(normalizedQuery))));
		Query prefixMatch = Query.of(q -> q.prefix(prefix -> prefix
			.field("suggestions.text.normalized")
			.value(normalizedQuery)));
		Query containsMatch = Query.of(q -> q.match(match -> match
			.field("suggestions.text.autocomplete")
			.query(FieldValue.of(normalizedQuery))));
		Query autocompleteQuery = Query.of(q -> q.bool(bool -> bool
			.should(should -> should.constantScore(score -> score
				.filter(exactMatch)
				.boost(100.0f)))
			.should(should -> should.constantScore(score -> score
				.filter(prefixMatch)
				.boost(10.0f)))
			.should(should -> should.constantScore(score -> score
				.filter(containsMatch)
				.boost(1.0f)))
			.minimumShouldMatch("1")
			.mustNot(mustNot -> mustNot.term(term -> term
				.field("type")
				.value(FieldValue.of(TV_SERIES_TYPE))))));

		try {
			return openSearchClient.search(request -> request
					.index(properties.getContentAutocompleteIndex())
					.size(limit)
					.query(autocompleteQuery)
					.sort(sort -> sort.score(score -> score.order(
						org.opensearch.client.opensearch._types.SortOrder.Desc)))
					.sort(sort -> sort.field(field -> field
						.field("contentId")
						.order(org.opensearch.client.opensearch._types.SortOrder.Asc))),
				ContentAutocompleteDocument.class)
				.hits().hits().stream()
				.flatMap(hit -> {
					ContentAutocompleteDocument document = hit.source();
					if (document == null || document.getSuggestions() == null) {
						return java.util.stream.Stream.empty();
					}
					UUID contentId = document.getContentId() == null
						? UUID.fromString(hit.id())
						: document.getContentId();
					return document.getSuggestions().stream()
						.map(term -> toCandidate(contentId, term, normalizedQuery))
						.filter(java.util.Objects::nonNull);
				})
				.toList();
		} catch (IOException | OpenSearchException | IllegalArgumentException exception) {
			throw new ContentSearchUnavailableException(exception);
		}
	}

	private ContentAutocompleteCandidate toCandidate(
		UUID contentId,
		ContentAutocompleteTerm term,
		String query
	) {
		if (term == null || term.text() == null) {
			return null;
		}
		String text = term.text().toLowerCase(Locale.ROOT);
		if (!text.contains(query)) {
			return null;
		}
		int matchRank = text.equals(query) ? 1 : text.startsWith(query) ? 2 : 3;
		return new ContentAutocompleteCandidate(contentId, term.text(), term.type(), matchRank);
	}

	private List<UUID> search(String keyword, String contentType) {
		Query query = Query.of(q -> q.bool(bool -> {
			bool.must(must -> must.multiMatch(multiMatch -> multiMatch
				.query(keyword)
				.fields(
					"title^3",
					"description",
					"genres",
					"tags",
					"sportTypeCode",
					"sportType",
					"leagueName",
					"season",
					"homeTeamName",
					"awayTeamName"
				)));
			bool.mustNot(mustNot -> mustNot.term(term -> term
				.field("hidden")
				.value(FieldValue.of(true))));
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
		} catch (IOException | OpenSearchException | IllegalArgumentException exception) {
			throw new ContentSearchUnavailableException(exception);
		}
	}
}
