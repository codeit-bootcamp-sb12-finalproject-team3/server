package com.moduplaylist.infrastructure.opensearch.content;

import com.moduplaylist.infrastructure.opensearch.config.OpenSearchProperties;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mopl.opensearch", name = "enabled", havingValue = "true")
public class ContentVectorRepository {

    private static final int ID_SCAN_PAGE_SIZE = 500;

    private final OpenSearchClient openSearchClient;
    private final OpenSearchProperties properties;

    public Optional<ContentVectorDocument> findById(UUID contentId) {
        try {
            var response = openSearchClient.get(request -> request
                            .index(properties.getContentIndex())
                            .id(contentId.toString()),
                    ContentVectorDocument.class);
            if (!response.found()) {
                return Optional.empty();
            }
            return Optional.ofNullable(response.source());
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "콘텐츠 벡터를 OpenSearch에서 조회하지 못했습니다. contentId="
                            + contentId,
                    exception
            );
        }
    }

    public void upsert(ContentVectorDocument document) {
        try {
            openSearchClient.index(request -> request
                    .index(properties.getContentIndex())
                    .id(document.getContentId().toString())
                    .document(document));
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "콘텐츠 벡터를 OpenSearch에 저장하지 못했습니다. contentId="
                            + document.getContentId(),
                    exception
            );
        }
    }

    public List<UUID> findAllIds() {
        List<UUID> contentIds = new ArrayList<>();
        List<String> searchAfter = List.of();

        try {
            while (true) {
                List<String> currentSearchAfter = searchAfter;
                var response = openSearchClient.search(request -> {
                    request.index(properties.getContentIndex())
                            .size(ID_SCAN_PAGE_SIZE)
                            .source(source -> source.filter(filter -> filter
                                    .includes("contentId")))
                            .query(query -> query.matchAll(matchAll -> matchAll))
                            .sort(sort -> sort.field(field -> field.field("contentId")));
                    if (!currentSearchAfter.isEmpty()) {
                        request.searchAfter(currentSearchAfter);
                    }
                    return request;
                }, ContentVectorDocument.class);

                var hits = response.hits().hits();
                for (var hit : hits) {
                    ContentVectorDocument source = hit.source();
                    contentIds.add(source == null || source.getContentId() == null
                            ? UUID.fromString(hit.id())
                            : source.getContentId());
                }
                if (hits.size() < ID_SCAN_PAGE_SIZE) {
                    return List.copyOf(contentIds);
                }
                searchAfter = hits.get(hits.size() - 1).sort();
            }
        } catch (IOException exception) {
            throw new IllegalStateException("콘텐츠 벡터 ID 목록을 조회하지 못했습니다.", exception);
        }
    }

    public void deleteById(UUID contentId) {
        try {
            openSearchClient.delete(request -> request
                    .index(properties.getContentIndex())
                    .id(contentId.toString()));
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "콘텐츠 벡터를 OpenSearch에서 삭제하지 못했습니다. contentId="
                            + contentId,
                    exception
            );
        }
    }
}
