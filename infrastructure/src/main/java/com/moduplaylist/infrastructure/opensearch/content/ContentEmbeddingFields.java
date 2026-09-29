package com.moduplaylist.infrastructure.opensearch.content;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContentEmbeddingFields {

    private float[] embedding;
    private String embeddingModel;
    private Instant sourceUpdatedAt;
    private Instant embeddedAt;
}
