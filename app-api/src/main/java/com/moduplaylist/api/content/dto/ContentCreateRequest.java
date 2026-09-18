package com.moduplaylist.api.content.dto;

import com.moduplaylist.core.content.entity.ContentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
	private Integer seasonCount;

	@PositiveOrZero
	private Integer episodeCount;

	@Size(max = 50)
	private String sportType;
	private Instant scheduledAt;
	private String league;
	private String season;
	private String round;
	private String homeTeam;
	private String awayTeam;
	private String venue;
	private String country;
	@PositiveOrZero
	private Integer homeScore;
	@PositiveOrZero
	private Integer awayScore;

	private String description;

	private LocalDate releaseDate;

	@Positive
	private Integer runtime;

	private Map<String, Object> metadata;

	private List<@NotNull UUID> genreIds;

	private List<@NotBlank @Size(max = 100) String> manualTags;

	private List<@NotNull @Valid ContentCastRequest> casts;

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

	public void setSeasonCount(Integer seasonCount) {
		this.seasonCount = seasonCount;
	}

	public void setEpisodeCount(Integer episodeCount) {
		this.episodeCount = episodeCount;
	}

	public void setSportType(String sportType) {
		this.sportType = strip(sportType);
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
		this.description = description;
	}

	public void setReleaseDate(LocalDate releaseDate) {
		this.releaseDate = releaseDate;
	}

	public void setRuntime(Integer runtime) {
		this.runtime = runtime;
	}

	public void setMetadata(Map<String, Object> metadata) {
		this.metadata = metadata;
	}

	public void setGenreIds(List<UUID> genreIds) {
		this.genreIds = genreIds;
	}

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

	private static String strip(String value) {
		return value == null ? null : value.strip();
	}
}
