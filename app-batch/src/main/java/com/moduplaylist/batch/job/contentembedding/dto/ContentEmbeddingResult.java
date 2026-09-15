package com.moduplaylist.batch.job.contentembedding.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ContentEmbeddingResult{
    private UUID contentId;
    private String embeddingText;
    private int dimensions;

}
