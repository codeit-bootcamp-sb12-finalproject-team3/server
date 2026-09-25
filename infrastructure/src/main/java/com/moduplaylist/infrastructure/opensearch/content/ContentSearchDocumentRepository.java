package com.moduplaylist.infrastructure.opensearch.content;

import com.moduplaylist.infrastructure.opensearch.config.OpenSearchProperties;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mopl.opensearch", name = "enabled", havingValue = "true")
public class ContentSearchDocumentRepository {

    private final OpenSearchClient openSearchClient;
    private final OpenSearchProperties properties;

    public Optional<ContentSearchDocument> findById(UUID contentId) {
        try {
            var response = openSearchClient.get(request -> request
                            .index(properties.getContentIndex())
                            .id(contentId.toString()),
                    ContentSearchDocument.class);
            return response.found()
                    ? Optional.ofNullable(response.source())
                    : Optional.empty();
        } catch (IOException exception) {
            throw failure("조회", contentId, exception);
        }
    }

    public void upsertSearchFields(ContentSearchFields fields) {
        try {
            openSearchClient.update(request -> request
                    .index(properties.getContentIndex())
                    .id(fields.getContentId().toString())
                    .doc(fields)
                    .docAsUpsert(true)
                    .detectNoop(true), ContentSearchDocument.class);
        } catch (IOException exception) {
            throw failure("검색 필드 저장", fields.getContentId(), exception);
        }
    }

    public void updateEmbedding(UUID contentId, ContentEmbeddingFields fields) {
        try {
            openSearchClient.update(request -> request
                    .index(properties.getContentIndex())
                    .id(contentId.toString())
                    .doc(fields)
                    .docAsUpsert(true)
                    .detectNoop(true), ContentSearchDocument.class);
        } catch (IOException exception) {
            throw failure("임베딩 저장", contentId, exception);
        }
    }

    public void deleteById(UUID contentId) {
        try {
            openSearchClient.delete(request -> request
                    .index(properties.getContentIndex())
                    .id(contentId.toString()));
        } catch (IOException exception) {
            throw failure("삭제", contentId, exception);
        }
    }

    private IllegalStateException failure(String operation, UUID contentId, IOException cause) {
        return new IllegalStateException(
                "콘텐츠 검색 문서 " + operation + "에 실패했습니다. contentId=" + contentId,
                cause
        );
    }
}
