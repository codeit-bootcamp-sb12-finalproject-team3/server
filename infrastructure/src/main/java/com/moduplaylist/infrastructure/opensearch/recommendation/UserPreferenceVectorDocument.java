package com.moduplaylist.infrastructure.opensearch.recommendation;

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
public class UserPreferenceVectorDocument {

    private UUID userId;
    private List<GenreScoreDocument> genreScores;
    private List<TagScoreDocument> tagScores;
    private float[] embedding;
    private String embeddingModel;
    private Instant preferenceUpdatedAt;
    private Instant embeddedAt;
}
