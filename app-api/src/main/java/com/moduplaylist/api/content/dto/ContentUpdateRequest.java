package com.moduplaylist.api.content.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.openapitools.jackson.nullable.JsonNullable;

@Getter
@NoArgsConstructor
public class ContentUpdateRequest {

	private JsonNullable<@NotBlank @Size(max = 255) String> title = JsonNullable.undefined();
	private JsonNullable<@NotBlank String> description = JsonNullable.undefined();
	private JsonNullable<LocalDate> releaseDate = JsonNullable.undefined();
	private JsonNullable<@Positive Integer> runtime = JsonNullable.undefined();
	private JsonNullable<@PositiveOrZero Integer> seasonCount = JsonNullable.undefined();
	private JsonNullable<@PositiveOrZero Integer> episodeCount = JsonNullable.undefined();
	private JsonNullable<@NotNull UUID> sportTypeId = JsonNullable.undefined();
	private JsonNullable<Instant> scheduledAt = JsonNullable.undefined();
	private JsonNullable<@Size(max = 255) String> league = JsonNullable.undefined();
	private JsonNullable<@Size(max = 100) String> season = JsonNullable.undefined();
	private JsonNullable<@Size(max = 100) String> round = JsonNullable.undefined();
	private JsonNullable<@NotBlank @Size(max = 255) String> homeTeam = JsonNullable.undefined();
	private JsonNullable<@NotBlank @Size(max = 255) String> awayTeam = JsonNullable.undefined();
	private JsonNullable<@Size(max = 255) String> venue = JsonNullable.undefined();
	private JsonNullable<@Size(max = 100) String> country = JsonNullable.undefined();
	private JsonNullable<@PositiveOrZero Integer> homeScore = JsonNullable.undefined();
	private JsonNullable<@PositiveOrZero Integer> awayScore = JsonNullable.undefined();
	private JsonNullable<Map<String, Object>> metadata = JsonNullable.undefined();
	private JsonNullable<@NotNull List<@NotNull UUID>> genreIds = JsonNullable.undefined();
	private JsonNullable<@NotNull List<@NotBlank @Size(max = 100) String>> manualTags = JsonNullable.undefined();
	private JsonNullable<@NotNull List<@NotNull @Valid ContentCastRequest>> casts = JsonNullable.undefined();
	private JsonNullable<@NotNull List<@NotNull @Valid ContentPlatformCreateRequest>> platforms =
		JsonNullable.undefined();
	private boolean removeThumbnail;
	private final Set<String> unknownFields = new HashSet<>();

	public void setTitle(JsonNullable<String> title) {
		this.title = map(title, ContentUpdateRequest::strip);
	}

	public void setDescription(JsonNullable<String> description) {
		this.description = map(description, ContentUpdateRequest::strip);
	}

	public void setReleaseDate(JsonNullable<LocalDate> releaseDate) {
		this.releaseDate = requireWrapper(releaseDate);
	}

	public void setRuntime(JsonNullable<Integer> runtime) {
		this.runtime = requireWrapper(runtime);
	}

	public void setSeasonCount(JsonNullable<Integer> seasonCount) {
		this.seasonCount = requireWrapper(seasonCount);
	}

	public void setEpisodeCount(JsonNullable<Integer> episodeCount) {
		this.episodeCount = requireWrapper(episodeCount);
	}

	public void setSportTypeId(JsonNullable<UUID> sportTypeId) {
		this.sportTypeId = requireWrapper(sportTypeId);
	}

	public void setScheduledAt(JsonNullable<Instant> scheduledAt) {
		this.scheduledAt = requireWrapper(scheduledAt);
	}

	public void setLeague(JsonNullable<String> league) {
		this.league = map(league, ContentUpdateRequest::strip);
	}

	public void setSeason(JsonNullable<String> season) {
		this.season = map(season, ContentUpdateRequest::strip);
	}

	public void setRound(JsonNullable<String> round) {
		this.round = map(round, ContentUpdateRequest::strip);
	}

	public void setHomeTeam(JsonNullable<String> homeTeam) {
		this.homeTeam = map(homeTeam, ContentUpdateRequest::strip);
	}

	public void setAwayTeam(JsonNullable<String> awayTeam) {
		this.awayTeam = map(awayTeam, ContentUpdateRequest::strip);
	}

	public void setVenue(JsonNullable<String> venue) {
		this.venue = map(venue, ContentUpdateRequest::strip);
	}

	public void setCountry(JsonNullable<String> country) {
		this.country = map(country, ContentUpdateRequest::strip);
	}

	public void setHomeScore(JsonNullable<Integer> homeScore) {
		this.homeScore = requireWrapper(homeScore);
	}

	public void setAwayScore(JsonNullable<Integer> awayScore) {
		this.awayScore = requireWrapper(awayScore);
	}

	public void setMetadata(JsonNullable<Map<String, Object>> metadata) {
		this.metadata = requireWrapper(metadata);
	}

	public void setGenreIds(JsonNullable<List<UUID>> genreIds) {
		this.genreIds = requireWrapper(genreIds);
	}

	public void setManualTags(JsonNullable<List<String>> manualTags) {
		this.manualTags = map(
			manualTags,
			values -> values == null
				? null
				: values.stream().map(ContentUpdateRequest::strip).toList()
		);
	}

	public void setCasts(JsonNullable<List<ContentCastRequest>> casts) {
		this.casts = requireWrapper(casts);
	}

	public void setPlatforms(JsonNullable<List<ContentPlatformCreateRequest>> platforms) {
		this.platforms = requireWrapper(platforms);
	}

	public void setRemoveThumbnail(boolean removeThumbnail) {
		this.removeThumbnail = removeThumbnail;
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

	private static <T> JsonNullable<T> requireWrapper(JsonNullable<T> value) {
		return value == null ? JsonNullable.of(null) : value;
	}

	private static <T> JsonNullable<T> map(
		JsonNullable<T> value,
		java.util.function.UnaryOperator<T> mapper
	) {
		JsonNullable<T> wrapper = requireWrapper(value);
		return wrapper.isPresent()
			? JsonNullable.of(mapper.apply(wrapper.orElse(null)))
			: JsonNullable.undefined();
	}
}
