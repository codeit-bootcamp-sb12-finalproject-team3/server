package com.moduplaylist.batch.job.contentembedding.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ContentEmbeddingSource{
    private String title;
    private String type;
    private String description;
    private List<String> genres;
    private List<String> tags;
}
