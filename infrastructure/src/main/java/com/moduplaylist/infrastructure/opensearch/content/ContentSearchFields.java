package com.moduplaylist.infrastructure.opensearch.content;

import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContentSearchFields {

    private UUID contentId;
    private String type;
    private String title;
    private String seriesTitle;
    private String originalTitle;
    private String englishTitle;
    private List<String> castNames;
    private String description;
    private Boolean hidden;
    private List<String> genres;
    private List<String> tags;
    private List<String> tagSearch;
    private String sportTypeCode;
    private String sportType;
    private String leagueName;
    private String season;
    private String homeTeamName;
    private String awayTeamName;
}
