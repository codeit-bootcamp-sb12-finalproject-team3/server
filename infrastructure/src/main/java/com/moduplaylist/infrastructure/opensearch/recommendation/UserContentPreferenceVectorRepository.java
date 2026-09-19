package com.moduplaylist.infrastructure.opensearch.recommendation;

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
public class UserContentPreferenceVectorRepository {

    private final OpenSearchClient openSearchClient;
    private final OpenSearchProperties properties;

    public Optional<UserContentPreferenceVectorDocument> findById(UUID userId) {
        try {
            var response = openSearchClient.get(request -> request
                            .index(properties.getUserContentPreferenceIndex())
                            .id(userId.toString()),
                    UserContentPreferenceVectorDocument.class);
            if (!response.found()) {
                return Optional.empty();
            }
            return Optional.ofNullable(response.source());
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "사용자 선호 벡터를 OpenSearch에서 조회하지 못했습니다. userId="
                            + userId,
                    exception
            );
        }
    }

    public void upsert(UserContentPreferenceVectorDocument document) {
        try {
            openSearchClient.index(request -> request
                    .index(properties.getUserContentPreferenceIndex())
                    .id(document.getUserId().toString())
                    .document(document));
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "사용자 선호 벡터를 OpenSearch에 저장하지 못했습니다. userId="
                            + document.getUserId(),
                    exception
            );
        }
    }

    public void deleteById(UUID userId) {
        try {
            openSearchClient.delete(request -> request
                    .index(properties.getUserContentPreferenceIndex())
                    .id(userId.toString()));
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "사용자 선호 벡터를 OpenSearch에서 삭제하지 못했습니다. userId="
                            + userId,
                    exception
            );
        }
    }
}
