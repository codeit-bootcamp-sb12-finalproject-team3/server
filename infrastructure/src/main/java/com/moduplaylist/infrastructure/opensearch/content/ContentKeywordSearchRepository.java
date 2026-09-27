package com.moduplaylist.infrastructure.opensearch.content;

import com.moduplaylist.core.content.exception.ContentSearchUnavailableException;
import com.moduplaylist.infrastructure.opensearch.config.OpenSearchProperties;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.query_dsl.Operator;
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
	private static final int FUZZY_FALLBACK_RESULT_THRESHOLD = 10;
	private static final int MIN_FUZZY_QUERY_LENGTH = 3;
	private static final String TV_SERIES_TYPE = "tvSeries";
	private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");

	private final OpenSearchClient openSearchClient;
	private final OpenSearchProperties properties;

	public ContentKeywordSearchResult findContentIds(String keyword, String contentType) {
		return search(keyword, contentType);
	}

	public List<ContentAutocompleteCandidate> findAutocompleteCandidates(String query, int limit) {
		String normalizedQuery = normalizeAutocompleteText(query);
		String compactQuery = removeWhitespace(normalizedQuery);
		Query exactMatch = Query.of(q -> q.term(term -> term
			.field("suggestions.text.normalized")
			.value(FieldValue.of(normalizedQuery))));
		Query compactExactMatch = Query.of(q -> q.term(term -> term
			.field("suggestions.compactText.normalized")
			.value(FieldValue.of(compactQuery))));
		Query prefixMatch = Query.of(q -> q.prefix(prefix -> prefix
			.field("suggestions.text.normalized")
			.value(normalizedQuery)));
		Query compactPrefixMatch = Query.of(q -> q.prefix(prefix -> prefix
			.field("suggestions.compactText.normalized")
			.value(compactQuery)));
		Query containsMatch = Query.of(q -> q.match(match -> match
			.field("suggestions.text.autocomplete")
			.query(FieldValue.of(normalizedQuery))));
		Query compactContainsMatch = Query.of(q -> q.match(match -> match
			.field("suggestions.compactText.autocomplete")
			.query(FieldValue.of(compactQuery))));
		Query autocompleteQuery = Query.of(q -> q.bool(bool -> bool
			.should(should -> should.constantScore(score -> score
				.filter(exactMatch)
				.boost(100.0f)))
			.should(should -> should.constantScore(score -> score
				.filter(compactExactMatch)
				.boost(80.0f)))
			.should(should -> should.constantScore(score -> score
				.filter(prefixMatch)
				.boost(10.0f)))
			.should(should -> should.constantScore(score -> score
				.filter(compactPrefixMatch)
				.boost(8.0f)))
			.should(should -> should.constantScore(score -> score
				.filter(containsMatch)
				.boost(1.0f)))
			.should(should -> should.constantScore(score -> score
				.filter(compactContainsMatch)
				.boost(0.8f)))
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
						.map(term -> toCandidate(
							contentId, term, normalizedQuery, compactQuery))
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
		String query,
		String compactQuery
	) {
		if (term == null || term.text() == null) {
			return null;
		}
		String text = normalizeAutocompleteText(term.text());
		String compactText = term.compactText() == null
			? removeWhitespace(text)
			: normalizeAutocompleteText(term.compactText());
		boolean originalMatches = text.contains(query);
		boolean compactMatches = compactText.contains(compactQuery);
		if (!originalMatches && !compactMatches) {
			return null;
		}
		int matchRank = text.equals(query) || compactText.equals(compactQuery)
			? 1
			: text.startsWith(query) || compactText.startsWith(compactQuery) ? 2 : 3;
		return new ContentAutocompleteCandidate(contentId, term.text(), term.type(), matchRank);
	}

	private String normalizeAutocompleteText(String text) {
		String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
		return COMBINING_MARKS.matcher(decomposed)
			.replaceAll("")
			.toLowerCase(Locale.ROOT);
	}

	private String removeWhitespace(String text) {
		StringBuilder compact = new StringBuilder(text.length());
		text.codePoints()
			.filter(codePoint -> !Character.isWhitespace(codePoint))
			.forEach(compact::appendCodePoint);
		return compact.toString();
	}

	private ContentKeywordSearchResult search(String keyword, String contentType) {
		Query titleExact = scopedQuery(Query.of(q -> q.bool(bool -> bool
			.should(should -> should.term(term -> term
				.field("title.keyword")
				.value(FieldValue.of(keyword))))
			.should(should -> should.term(term -> term
				.field("seriesTitle.keyword")
				.value(FieldValue.of(keyword))))
			.should(should -> should.term(term -> term
				.field("englishTitle.keyword")
				.value(FieldValue.of(keyword))))
			.minimumShouldMatch("1"))), contentType);
		Query originalTitleExact = scopedQuery(Query.of(q -> q.term(term -> term
			.field("originalTitle.keyword")
			.value(FieldValue.of(keyword)))), contentType);
		Query titlePhrasePrefix = scopedQuery(Query.of(q -> q.bool(bool -> bool
			.should(should -> should.matchPhrase(match -> match
				.field("title")
				.query(keyword)
				.boost(30.0f)))
			.should(should -> should.matchPhrase(match -> match
				.field("seriesTitle")
				.query(keyword)
				.boost(30.0f)))
			.should(should -> should.matchPhrase(match -> match
				.field("englishTitle")
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
			.should(should -> should.matchPhrasePrefix(match -> match
				.field("englishTitle")
				.query(keyword)
				.maxExpansions(50)
				.boost(20.0f)))
			.minimumShouldMatch("1"))), contentType);
		Query synonymBm25 = Query.of(q -> q.multiMatch(multiMatch -> multiMatch
				.query(keyword)
				.analyzer("content_synonym_search")
				.fields(
					"title^10",
					"seriesTitle^10",
					"englishTitle^10",
					"originalTitle^8",
					"tagSearch^5"
				)));
		Query nativeBm25 = Query.of(q -> q.multiMatch(multiMatch -> multiMatch
				.query(keyword)
				.fields(
					"title^10",
					"title.compact^10",
					"seriesTitle^10",
					"seriesTitle.compact^10",
					"englishTitle^10",
					"englishTitle.compact^10",
					"originalTitle^8",
					"originalTitle.compact^8",
					"tagSearch^5",
					"tagSearch.compact^5",
					"castNames^3",
					"castNames.compact^3",
					"description",
					"genres",
					"genres.compact",
					"sportTypeCode",
					"sportTypeCode.compact",
					"sportType",
					"sportType.compact",
					"leagueName",
					"leagueName.compact",
					"season",
					"season.compact",
					"homeTeamName",
					"homeTeamName.compact",
					"awayTeamName",
					"awayTeamName.compact"
				)));
		Query bm25 = scopedQuery(Query.of(q -> q.disMax(disMax -> disMax
			.queries(synonymBm25, nativeBm25)
			.tieBreaker(0.0))), contentType);
		Query fuzzy = scopedQuery(Query.of(q -> q.multiMatch(multiMatch -> multiMatch
			.query(keyword)
			.fields(
				"title^5",
				"title.compact^5",
				"seriesTitle^5",
				"seriesTitle.compact^5",
				"originalTitle^4",
				"originalTitle.compact^4",
				"englishTitle^4",
				"englishTitle.compact^4",
				"castNames^3",
				"castNames.compact^3"
			)
			.fuzziness("AUTO")
			.prefixLength(1)
			.maxExpansions(30)
			.fuzzyTranspositions(true)
			.operator(Operator.And))), contentType);

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
			List<UUID> bm25Ids = groups.get(3);
			if (shouldUseFuzzyFallback(keyword, groups)) {
				bm25Ids = mergeBm25AndFuzzyResults(bm25Ids, searchIds(fuzzy));
			}
			return new ContentKeywordSearchResult(
				groups.get(0), groups.get(1), groups.get(2), bm25Ids);
		} catch (IOException | OpenSearchException | IllegalArgumentException exception) {
			throw new ContentSearchUnavailableException(exception);
		}
	}

	private boolean shouldUseFuzzyFallback(String keyword, List<List<UUID>> groups) {
		long queryLength = keyword.codePoints()
			.filter(codePoint -> !Character.isWhitespace(codePoint))
			.count();
		if (queryLength < MIN_FUZZY_QUERY_LENGTH) {
			return false;
		}
		LinkedHashSet<UUID> distinctIds = new LinkedHashSet<>();
		groups.forEach(distinctIds::addAll);
		return distinctIds.size() < FUZZY_FALLBACK_RESULT_THRESHOLD;
	}

	private List<UUID> searchIds(Query query) throws IOException {
		return openSearchClient.search(request -> request
				.index(properties.getContentIndex())
				.size(MAX_RESULTS)
				.source(source -> source.fetch(false))
				.query(query),
			Void.class)
			.hits().hits().stream()
			.map(hit -> UUID.fromString(hit.id()))
			.toList();
	}

	private List<UUID> mergeBm25AndFuzzyResults(
		List<UUID> bm25Ids,
		List<UUID> fuzzyIds
	) {
		LinkedHashSet<UUID> mergedIds = new LinkedHashSet<>(bm25Ids);
		mergedIds.addAll(fuzzyIds);
		return mergedIds.stream()
			.limit(MAX_RESULTS)
			.toList();
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
