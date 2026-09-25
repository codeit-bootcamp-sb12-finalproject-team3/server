package com.moduplaylist.infrastructure.opensearch.content;

import com.moduplaylist.core.content.entity.ContentType;
import java.util.List;
import java.util.UUID;

public record ContentIndexSource(
    UUID contentId,
    boolean indexable,
    ContentType type,
    String title,
    String originalTitle,
    String seriesTitle,
    Integer seasonNumber,
    String description,
    List<String> castNames,
    List<String> genres,
    List<String> tags,
    SportFields sport
) {

    public ContentIndexSource {
        castNames = castNames == null ? List.of() : List.copyOf(castNames);
        genres = genres == null ? List.of() : List.copyOf(genres);
        tags = tags == null ? List.of() : List.copyOf(tags);
    }

    public static ContentIndexSource notIndexable(UUID contentId) {
        return new ContentIndexSource(
                contentId, false, null, null, null, null, null, null,
                List.of(), List.of(), List.of(), null);
    }

    public record SportFields(
        String sportTypeCode,
        String sportType,
        String leagueName,
        String season,
        String homeTeamName,
        String awayTeamName
    ) {
    }
}
