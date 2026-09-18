package com.moduplaylist.api.content.dto;

import com.moduplaylist.core.content.entity.ContentType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContentResponse {

	private UUID id;
	private UUID parentContentId;
	private String title;
	private ContentType type;
	private Integer seasonNumber;
	private String sportType;
	private String thumbnailUrl;
	private LocalDate releaseDate;
	private Integer runtime;
	private BigDecimal averageRating;
	private long reviewCount;
	private long likeCount;

	@Builder.Default
	private List<GenreResponse> genres = List.of();

	@Builder.Default
	private List<TagResponse> tags = List.of();

	private String description;
	private Integer seasonCount;
	private Integer episodeCount;

	@Builder.Default
	private List<CastResponse> cast = List.of();

	private Map<String, Object> metadata;
	private SportEventResponse sportEvent;
	private Instant createdAt;
	private Instant updatedAt;
}
