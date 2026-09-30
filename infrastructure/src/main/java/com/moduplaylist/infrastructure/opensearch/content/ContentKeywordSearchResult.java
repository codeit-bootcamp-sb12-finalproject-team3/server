package com.moduplaylist.infrastructure.opensearch.content;

import java.util.List;
import java.util.UUID;

public record ContentKeywordSearchResult(
    List<UUID> titleExactIds,
    List<UUID> originalTitleExactIds,
    List<UUID> titlePhrasePrefixIds,
    List<UUID> bm25Ids
) {

    public ContentKeywordSearchResult {
        titleExactIds = List.copyOf(titleExactIds);
        originalTitleExactIds = List.copyOf(originalTitleExactIds);
        titlePhrasePrefixIds = List.copyOf(titlePhrasePrefixIds);
        bm25Ids = List.copyOf(bm25Ids);
    }
}
