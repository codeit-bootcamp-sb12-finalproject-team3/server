package com.moduplaylist.core.content.repository;

import com.moduplaylist.core.content.entity.Content;
import java.time.Instant;
import java.util.List;
import lombok.Getter;

public interface ContentQueryRepository {

    @Getter
    class SearchResult {
        private final List<Content> contents;
        private final long totalCount;
        private final boolean hasNext;
        private final Instant nextCursorLikedAt;

        public SearchResult(
                List<Content> contents,
                long totalCount,
                boolean hasNext,
                Instant nextCursorLikedAt) {
            this.contents = List.copyOf(contents);
            this.totalCount = totalCount;
            this.hasNext = hasNext;
            this.nextCursorLikedAt = nextCursorLikedAt;
        }
    }

    SearchResult search(ContentSearch request);

    SearchResult searchNewContents(NewContentSearch request);
}
