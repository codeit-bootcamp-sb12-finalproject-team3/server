package com.moduplaylist.api.content.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContentSummaryResponse {

	private UUID id;
	private UUID parentContentId;
	private String title;
	private String description;
	private ContentSummaryType type;
	private Integer seasonNumber;
	private Integer episodeCount;
	private String sportType;
	private String league;
	private String homeTeam;
	private String awayTeam;
	private String thumbnailUrl;
	private LocalDate releaseDate;
	private Integer runtime;
	private BigDecimal averageRating;
	private long reviewCount;
	private long likeCount;
	private boolean likedByMe;

	@Builder.Default
	private List<GenreResponse> genres = List.of();

	@Builder.Default
	private List<TagResponse> tags = List.of();
}
