package com.moduplaylist.infrastructure.opensearch.recommendation;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenreScoreDocument {

    private UUID genreId;
    private String genreName;
    private double score;
}
