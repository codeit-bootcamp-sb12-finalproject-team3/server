package com.moduplaylist.infrastructure.opensearch.playlist;

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
public class PlaylistVectorRepository {

    private final OpenSearchClient openSearchClient;
    private final OpenSearchProperties properties;

    public Optional<PlaylistVectorDocument> findById(UUID playlistId) {
        try {
            var response = openSearchClient.get(request -> request
                            .index(properties.getPlaylistIndex())
                            .id(playlistId.toString()),
                    PlaylistVectorDocument.class);
            if (!response.found()) {
                return Optional.empty();
            }
            return Optional.ofNullable(response.source());
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "플레이리스트 벡터를 OpenSearch에서 조회하지 못했습니다. playlistId="
                            + playlistId,
                    exception
            );
        }
    }

    public void upsert(PlaylistVectorDocument document) {
        try {
            openSearchClient.index(request -> request
                    .index(properties.getPlaylistIndex())
                    .id(document.getPlaylistId().toString())
                    .document(document));
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "플레이리스트 벡터를 OpenSearch에 저장하지 못했습니다. playlistId="
                            + document.getPlaylistId(),
                    exception
            );
        }
    }

    public void deleteById(UUID playlistId) {
        try {
            openSearchClient.delete(request -> request
                    .index(properties.getPlaylistIndex())
                    .id(playlistId.toString()));
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "플레이리스트 벡터를 OpenSearch에서 삭제하지 못했습니다. playlistId="
                            + playlistId,
                    exception
            );
        }
    }
}
