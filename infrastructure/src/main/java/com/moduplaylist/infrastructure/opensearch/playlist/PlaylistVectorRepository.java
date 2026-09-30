package com.moduplaylist.infrastructure.opensearch.playlist;

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
public class PlaylistVectorRepository {

    private static final int ID_SCAN_PAGE_SIZE = 500;

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

    public List<UUID> findAllIds() {
        List<UUID> playlistIds = new ArrayList<>();
        List<String> searchAfter = List.of();

        try {
            while (true) {
                List<String> currentSearchAfter = searchAfter;
                var response = openSearchClient.search(request -> {
                    request.index(properties.getPlaylistIndex())
                            .size(ID_SCAN_PAGE_SIZE)
                            .source(source -> source.filter(filter -> filter
                                    .includes("playlistId")))
                            .query(query -> query.matchAll(matchAll -> matchAll))
                            .sort(sort -> sort.field(field -> field.field("playlistId")));
                    if (!currentSearchAfter.isEmpty()) {
                        request.searchAfter(currentSearchAfter);
                    }
                    return request;
                }, PlaylistVectorDocument.class);

                var hits = response.hits().hits();
                for (var hit : hits) {
                    PlaylistVectorDocument source = hit.source();
                    playlistIds.add(source == null || source.getPlaylistId() == null
                            ? UUID.fromString(hit.id())
                            : source.getPlaylistId());
                }
                if (hits.size() < ID_SCAN_PAGE_SIZE) {
                    return List.copyOf(playlistIds);
                }
                searchAfter = hits.get(hits.size() - 1).sort();
            }
        } catch (IOException exception) {
            throw new IllegalStateException("플레이리스트 벡터 ID 목록을 조회하지 못했습니다.", exception);
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
