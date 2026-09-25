package com.moduplaylist.infrastructure.opensearch.content;

import com.moduplaylist.core.content.exception.ContentSearchUnavailableException;
import com.moduplaylist.infrastructure.opensearch.config.OpenSearchProperties;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.MsearchResponse;
import org.opensearch.client.opensearch.core.msearch.MultiSearchResponseItem;
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

	public ContentKeywordSearchResult findContentIds(String keyword, String contentType) {
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

	private ContentKeywordSearchResult search(String keyword, String contentType) {
		Query titleExact = scopedQuery(Query.of(q -> q.bool(bool -> bool
			.should(should -> should.term(term -> term
				.field("title.keyword")
				.value(FieldValue.of(keyword))
				.caseInsensitive(true)))
			.should(should -> should.term(term -> term
				.field("seriesTitle.keyword")
				.value(FieldValue.of(keyword))
				.caseInsensitive(true)))
			.minimumShouldMatch("1"))), contentType);
		Query originalTitleExact = scopedQuery(Query.of(q -> q.term(term -> term
			.field("originalTitle.keyword")
			.value(FieldValue.of(keyword))
			.caseInsensitive(true))), contentType);
		Query titlePhrasePrefix = scopedQuery(Query.of(q -> q.bool(bool -> bool
			.should(should -> should.matchPhrase(match -> match
				.field("title")
				.query(keyword)
				.boost(30.0f)))
			.should(should -> should.matchPhrase(match -> match
				.field("seriesTitle")
				.query(keyword)
				.boost(30.0f)))
			.should(should -> should.matchPhrasePrefix(match -> match
				.field("title")
				.query(keyword)
				.maxExpansions(50)
				.boost(20.0f)))
			.should(should -> should.matchPhrasePrefix(match -> match
				.field("seriesTitle")
				.query(keyword)
				.maxExpansions(50)
				.boost(20.0f)))
			.minimumShouldMatch("1"))), contentType);
		Query bm25 = scopedQuery(Query.of(q -> q.bool(bool -> bool
			.should(should -> should.multiMatch(multiMatch -> multiMatch
				.query(keyword)
				.analyzer("content_synonym_search")
				.fields("title^10", "seriesTitle^10", "originalTitle^8", "tagSearch^5")))
			.should(should -> should.multiMatch(multiMatch -> multiMatch
				.query(keyword)
				.fields(
					"title^10",
					"seriesTitle^10",
					"originalTitle^8",
					"tagSearch^5",
					"castNames^3",
					"description",
					"genres",
					"sportTypeCode",
					"sportType",
					"leagueName",
					"season",
					"homeTeamName",
					"awayTeamName"
				)))
			.minimumShouldMatch("1"))), contentType);

		try {
			MsearchResponse<Void> response = openSearchClient.msearch(request -> request
					.index(properties.getContentIndex())
					.searches(item -> searchItem(item, titleExact))
					.searches(item -> searchItem(item, originalTitleExact))
					.searches(item -> searchItem(item, titlePhrasePrefix))
					.searches(item -> searchItem(item, bm25)),
				Void.class);
			List<List<UUID>> groups = new ArrayList<>(4);
			for (MultiSearchResponseItem<Void> item : response.responses()) {
				if (item.isFailure()) {
					throw new ContentSearchUnavailableException();
				}
				groups.add(item.result().hits().hits().stream()
					.map(hit -> UUID.fromString(hit.id()))
					.toList());
			}
			if (groups.size() != 4) {
				throw new ContentSearchUnavailableException();
			}
			return new ContentKeywordSearchResult(
				groups.get(0), groups.get(1), groups.get(2), groups.get(3));
		} catch (IOException | OpenSearchException | IllegalArgumentException exception) {
			throw new ContentSearchUnavailableException(exception);
		}
	}

	private org.opensearch.client.util.ObjectBuilder<
		org.opensearch.client.opensearch.core.msearch.RequestItem> searchItem(
		org.opensearch.client.opensearch.core.msearch.RequestItem.Builder item,
		Query query
	) {
		return item
			.header(header -> header)
			.body(body -> body
				.size(MAX_RESULTS)
				.source(source -> source.fetch(false))
				.query(query));
	}

	private Query scopedQuery(Query relevanceQuery, String contentType) {
		return Query.of(q -> q.bool(bool -> {
			bool.must(relevanceQuery);
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
	}
}
