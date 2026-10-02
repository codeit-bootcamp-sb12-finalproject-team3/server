package com.moduplaylist.core.content.ai;

/** A source-backed, spoiler-safe fact for content tagging. */
public record ContentExternalEvidence(
    Category category,
    String text,
    Scope scope,
    String sourceTitle,
    String sourceUrl
) {
    public enum Category {
        SUBGENRE,
        CORE_MOTIF,
        FRANCHISE,
        NARRATIVE,
        RELATIONSHIP,
        SOURCE_FORMAT,
        TONE
    }

    public enum Scope {
        MOVIE,
        SERIES,
        SEASON
    }
}
