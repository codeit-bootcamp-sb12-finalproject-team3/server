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
    List<String> seriesTagCandidates
) { }
