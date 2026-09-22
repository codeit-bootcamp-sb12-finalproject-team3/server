package com.moduplaylist.infrastructure.recommendation.embedding.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileEmbeddingResult {

    private UUID userId;
    private String embeddingText;
    private float[] embedding;
    private int dimensions;
}
