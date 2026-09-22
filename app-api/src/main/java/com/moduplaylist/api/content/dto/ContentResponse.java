package com.moduplaylist.api.content.dto;

import com.moduplaylist.core.content.entity.ContentType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContentResponse {

	private UUID id;
	private ContentType type;
	private String title;
	private String description;
	private String thumbnailUrl;
	private LocalDate releaseDate;
	private BigDecimal averageRating;
	private long reviewCount;
	private long likeCount;
	private String originalTitle;

	@Builder.Default
	private List<GenreResponse> genres = List.of();

	@Builder.Default
	private List<TagResponse> tags = List.of();

	@Builder.Default
	private List<CastResponse> cast = List.of();

	private MovieDetail movie;
	private TvSeasonDetail tvSeason;
	private SportDetail sport;
	private Instant createdAt;
	private Instant updatedAt;
}
