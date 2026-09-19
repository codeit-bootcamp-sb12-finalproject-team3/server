package com.moduplaylist.api.content.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.moduplaylist.core.content.entity.ContentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ContentCreateRequest {

	@NotBlank
	@Size(max = 255)
	private String title;

	@NotNull
	private ContentType type;

	private UUID parentContentId;

	@PositiveOrZero
	private Integer seasonNumber;

	@PositiveOrZero
	private Integer episodeCount;

	private UUID sportTypeId;
	private Instant scheduledAt;
	@Size(max = 255)
	private String league;
	@Size(max = 100)
	private String season;
	@Size(max = 100)
	private String round;
	@Size(max = 255)
	private String homeTeam;
	@Size(max = 255)
	private String awayTeam;
	@Size(max = 255)
	private String venue;
	@Size(max = 100)
	private String country;
	@PositiveOrZero
	private Integer homeScore;
	@PositiveOrZero
	private Integer awayScore;

	private String description;

	private LocalDate releaseDate;

	@Positive
	private Integer runtime;

	@Size(max = 255)
	private String originalTitle;

	private List<@NotNull UUID> genreIds;

	@JsonProperty("tags")
	private List<@NotBlank @Size(max = 100) String> manualTags;

	private List<@NotNull @Valid ContentCastRequest> casts;

	private List<@NotNull @Valid ContentPlatformCreateRequest> platforms;

	@Valid
	@Size(min = 1, max = 15)
	private List<@NotNull SeasonCreateRequest> seasons;

	private boolean duplicateConfirmed;

	private final Set<String> unknownFields = new HashSet<>();

	public void setTitle(String title) {
		this.title = strip(title);
	}

	public void setType(ContentType type) {
		this.type = type;
	}

	public void setParentContentId(UUID parentContentId) {
		this.parentContentId = parentContentId;
	}

	public void setSeasonNumber(Integer seasonNumber) {
		this.seasonNumber = seasonNumber;
	}

	public void setEpisodeCount(Integer episodeCount) {
		this.episodeCount = episodeCount;
	}

	public void setSportTypeId(UUID sportTypeId) {
		this.sportTypeId = sportTypeId;
	}

	public void setScheduledAt(Instant scheduledAt) { this.scheduledAt = scheduledAt; }
	public void setLeague(String league) { this.league = strip(league); }
	public void setSeason(String season) { this.season = strip(season); }
	public void setRound(String round) { this.round = strip(round); }
	public void setHomeTeam(String homeTeam) { this.homeTeam = strip(homeTeam); }
	public void setAwayTeam(String awayTeam) { this.awayTeam = strip(awayTeam); }
	public void setVenue(String venue) { this.venue = strip(venue); }
	public void setCountry(String country) { this.country = strip(country); }
	public void setHomeScore(Integer homeScore) { this.homeScore = homeScore; }
	public void setAwayScore(Integer awayScore) { this.awayScore = awayScore; }

	public void setDescription(String description) {
		this.description = strip(description);
	}

	public void setReleaseDate(LocalDate releaseDate) {
		this.releaseDate = releaseDate;
	}

	public void setRuntime(Integer runtime) {
		this.runtime = runtime;
	}

	public void setOriginalTitle(String originalTitle) {
		this.originalTitle = strip(originalTitle);
	}

	public void setGenreIds(List<UUID> genreIds) {
		this.genreIds = genreIds;
	}

	@JsonProperty("tags")
	public void setManualTags(List<String> manualTags) {
		this.manualTags = manualTags == null
			? null
			: manualTags.stream()
				.map(ContentCreateRequest::strip)
				.toList();
	}

	public void setCasts(List<ContentCastRequest> casts) {
		this.casts = casts;
	}

	public void setPlatforms(List<ContentPlatformCreateRequest> platforms) {
		this.platforms = platforms;
	}

	public void setSeasons(List<SeasonCreateRequest> seasons) { this.seasons = seasons; }

	public void setDuplicateConfirmed(boolean duplicateConfirmed) {
		this.duplicateConfirmed = duplicateConfirmed;
	}

	@JsonAnySetter
	public void addUnknownField(String name, Object value) {
		unknownFields.add(name);
	}

	@AssertTrue(message = "허용되지 않은 필드가 포함되어 있습니다.")
	public boolean isKnownFieldsOnly() {
		return unknownFields.isEmpty();
	}

	private static String strip(String value) {
		return value == null ? null : value.strip();
	}
}
