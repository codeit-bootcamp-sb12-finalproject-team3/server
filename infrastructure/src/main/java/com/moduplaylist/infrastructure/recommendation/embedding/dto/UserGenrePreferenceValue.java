package com.moduplaylist.infrastructure.recommendation.embedding.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserGenrePreferenceValue {

    private UUID genreId;
    private String genreName;
    private double score;
    private Instant scoreUpdatedAt;
}
