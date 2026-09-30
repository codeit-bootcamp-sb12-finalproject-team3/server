package com.moduplaylist.infrastructure.opensearch.playlist;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistVectorDocument {

    private UUID playlistId;
    private UUID ownerId;
    private String title;
    private String description;
    private List<String> genres;
    private List<String> tags;
    private float[] embedding;
    private String embeddingModel;
    private Instant sourceUpdatedAt;
    private Instant embeddedAt;
}
