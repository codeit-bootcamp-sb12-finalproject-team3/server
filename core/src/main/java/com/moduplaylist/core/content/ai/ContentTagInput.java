package com.moduplaylist.core.content.ai;

import java.util.List;

/** Only public content data may cross the model boundary. */
public record ContentTagInput(
    String type,
    String title,
    List<String> genres,
    String description,
    List<String> tmdbKeywords,
    String keywordScope,
    List<String> currentTags,
    List<String> seriesTagCandidates,
    List<ContentExternalEvidence> externalEvidence
) {
    /** Compatibility constructor for callers that do not provide research evidence. */
    public ContentTagInput(
        String type,
        String title,
        List<String> genres,
        String description,
        List<String> tmdbKeywords,
        String keywordScope,
        List<String> currentTags,
        List<String> seriesTagCandidates
    ) {
        this(type, title, genres, description, tmdbKeywords, keywordScope, currentTags,
            seriesTagCandidates, List.of());
    }
}
