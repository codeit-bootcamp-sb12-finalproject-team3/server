package com.moduplaylist.infrastructure.opensearch.content;

import com.moduplaylist.infrastructure.opensearch.config.OpenSearchProperties;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mopl.opensearch", name = "enabled", havingValue = "true")
public class ContentVectorRepository {

    private final OpenSearchClient openSearchClient;
    private final OpenSearchProperties properties;

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
}
