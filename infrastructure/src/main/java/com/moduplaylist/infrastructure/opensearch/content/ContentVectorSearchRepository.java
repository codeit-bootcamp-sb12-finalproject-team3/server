package com.moduplaylist.infrastructure.opensearch.content;

import com.moduplaylist.infrastructure.opensearch.config.OpenSearchProperties;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mopl.opensearch", name = "enabled", havingValue = "true")
public class ContentVectorSearchRepository {

    private static final String EMBEDDING_FIELD = "embedding";
    private static final String TYPE_FIELD = "type";
    private static final String TV_SERIES_TYPE = "tvSeries";
    private static final String SPORT_TYPE = "sport";

    private final OpenSearchClient openSearchClient;
    private final OpenSearchProperties properties;

    public List<ContentSimilarityCandidate> findNearest(
            float[] queryVector,
            Collection<UUID> excludedContentIds,
            int limit
    ) {
        validate(queryVector, limit);
        Query filter = buildFilter(excludedContentIds);

        try {
            SearchResponse<Void> response = openSearchClient.search(request -> request
                            .index(properties.getContentIndex())
                            .size(limit)
                            .source(source -> source.fetch(false))
                            .query(query -> query.knn(knn -> knn
                                    .field(EMBEDDING_FIELD)
                                    .vector(queryVector)
                                    .k(limit)
                                    .filter(filter))),
                    Void.class);

            return response.hits().hits().stream()
                    .map(hit -> new ContentSimilarityCandidate(
                            UUID.fromString(hit.id()),
                            Objects.requireNonNullElse(hit.score(), 0.0)
                    ))
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("콘텐츠 유사도 검색에 실패했습니다.", exception);
        }
    }

    private Query buildFilter(Collection<UUID> excludedContentIds) {
        List<String> excludedIds = excludedContentIds == null
                ? List.of()
                : excludedContentIds.stream()
                .map(UUID::toString)
                .toList();

        return Query.of(query -> query.bool(bool -> {

            bool.mustNot(typeQuery -> typeQuery.term(term -> term
                    .field(TYPE_FIELD)
                    .value(FieldValue.of(TV_SERIES_TYPE))));

            bool.mustNot(typeQuery -> typeQuery.term(term -> term
                    .field(TYPE_FIELD)
                    .value(FieldValue.of(SPORT_TYPE))));

            if (!excludedIds.isEmpty()) {
                bool.mustNot(idsQuery ->
                        idsQuery.ids(ids -> ids.values(excludedIds)));
            }

            return bool;
        }));
    }

    private void validate(float[] queryVector, int limit) {
        if (queryVector == null || queryVector.length == 0) {
            throw new IllegalArgumentException("검색할 사용자 벡터는 필수입니다.");
        }
        if (limit < 1 || limit > 10_000) {
            throw new IllegalArgumentException("벡터 검색 개수는 1부터 10000 사이여야 합니다.");
        }
    }
}
