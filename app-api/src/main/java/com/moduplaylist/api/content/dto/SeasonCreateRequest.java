package com.moduplaylist.api.content.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SeasonCreateRequest {

	@NotNull
	@PositiveOrZero
	private Integer seasonNumber;

	@NotBlank
	@Size(max = 255)
	private String title;

	@NotBlank
	private String description;

	@Pattern(regexp = "[A-Za-z0-9_-]{1,50}")
	private String thumbnailKey;

	private LocalDate releaseDate;

	@PositiveOrZero
	private Integer episodeCount;

	private Map<String, Object> metadata;

	private List<@NotNull @Valid ContentCastRequest> casts;

	@NotNull
	@Size(min = 1)
	private List<@NotNull UUID> genreIds;

	@JsonProperty("tags")
	private List<@NotBlank @Size(max = 100) String> tags;

	private List<@NotNull @Valid ContentPlatformCreateRequest> platforms;

	public void setSeasonNumber(Integer seasonNumber) { this.seasonNumber = seasonNumber; }
	public void setTitle(String title) { this.title = strip(title); }
	public void setDescription(String description) { this.description = strip(description); }
	public void setThumbnailKey(String thumbnailKey) { this.thumbnailKey = strip(thumbnailKey); }
	public void setReleaseDate(LocalDate releaseDate) { this.releaseDate = releaseDate; }
	public void setEpisodeCount(Integer episodeCount) { this.episodeCount = episodeCount; }
	public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
	public void setCasts(List<ContentCastRequest> casts) { this.casts = casts; }
	public void setGenreIds(List<UUID> genreIds) { this.genreIds = genreIds; }
	@JsonProperty("tags")
	public void setTags(List<String> tags) {
		this.tags = tags == null ? null : tags.stream().map(SeasonCreateRequest::strip).toList();
	}
	public void setPlatforms(List<ContentPlatformCreateRequest> platforms) { this.platforms = platforms; }

	private static String strip(String value) {
		return value == null ? null : value.strip();
	}
}
