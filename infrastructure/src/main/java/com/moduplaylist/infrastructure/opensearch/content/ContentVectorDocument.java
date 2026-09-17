package com.moduplaylist.infrastructure.opensearch.content;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentVectorDocument {

    private UUID contentId;
    private String type;
    private String title;
    private String description;
    private List<String> genres;
    private List<String> tags;
    private float[] embedding;
    private String embeddingModel;
    private Instant sourceUpdatedAt;
    private Instant embeddedAt;
}
