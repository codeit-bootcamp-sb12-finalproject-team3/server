package com.moduplaylist.core.content.ai;

/** Public identifiers used to find information about one movie or TV season.
 * For a TV season, tmdbId is the parent TV series ID and seasonNumber identifies the season.
 */
public record ContentResearchInput(
    String type,
    String title,
    String originalTitle,
    String englishTitle,
    Integer releaseYear,
    Integer seasonNumber,
    String seriesTitle,
    Integer tmdbId
) { }
