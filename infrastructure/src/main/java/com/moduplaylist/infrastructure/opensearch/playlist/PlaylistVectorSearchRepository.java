package com.moduplaylist.infrastructure.opensearch.playlist;

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
public class PlaylistVectorSearchRepository {

    private static final String EMBEDDING_FIELD = "embedding";
    private static final String OWNER_ID_FIELD = "ownerId";

    private final OpenSearchClient openSearchClient;
    private final OpenSearchProperties properties;

    public List<PlaylistSimilarityCandidate> findNearest(
            float[] queryVector,
            UUID userId,
            Collection<UUID> subscribedPlaylistIds,
            int limit
    ) {
        validate(queryVector, userId, limit);
        Query filter = buildFilter(userId, subscribedPlaylistIds);

        try {
            SearchResponse<Void> response = openSearchClient.search(request -> request
                            .index(properties.getPlaylistIndex())
                            .size(limit)
                            .source(source -> source.fetch(false))
                            .query(query -> query.knn(knn -> knn
                                    .field(EMBEDDING_FIELD)
                                    .vector(queryVector)
                                    .k(limit)
                                    .filter(filter))),
                    Void.class);

            return response.hits().hits().stream()
                    .map(hit -> new PlaylistSimilarityCandidate(
                            UUID.fromString(hit.id()),
                            Objects.requireNonNullElse(hit.score(), 0.0)
                    ))
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("플레이리스트 유사도 검색에 실패했습니다.", exception);
        }
    }

    private Query buildFilter(UUID userId, Collection<UUID> subscribedPlaylistIds) {
        List<String> excludedIds = subscribedPlaylistIds == null
                ? List.of()
                : subscribedPlaylistIds.stream()
                        .map(UUID::toString)
                        .toList();

        return Query.of(query -> query.bool(bool -> {
            bool.mustNot(ownerQuery -> ownerQuery.term(term -> term
                    .field(OWNER_ID_FIELD)
                    .value(FieldValue.of(userId.toString()))));

            if (!excludedIds.isEmpty()) {
                bool.mustNot(idsQuery -> idsQuery.ids(ids -> ids.values(excludedIds)));
            }

            return bool;
        }));
    }

    private void validate(float[] queryVector, UUID userId, int limit) {
        if (queryVector == null || queryVector.length == 0) {
            throw new IllegalArgumentException("검색할 사용자 플레이리스트 벡터는 필수입니다.");
        }
        Objects.requireNonNull(userId, "userId must not be null");
        if (limit < 1 || limit > 10_000) {
            throw new IllegalArgumentException("벡터 검색 개수는 1부터 10000 사이여야 합니다.");
        }
    }
}
