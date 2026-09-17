package com.moduplaylist.api.content.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.openapitools.jackson.nullable.JsonNullable;

@Getter
@NoArgsConstructor
public class ContentUpdateRequest {

	private JsonNullable<@NotBlank @Size(max = 255) String> title = JsonNullable.undefined();
	private JsonNullable<String> description = JsonNullable.undefined();
	private JsonNullable<@NotNull UUID> parentContentId = JsonNullable.undefined();
	private JsonNullable<@NotNull @PositiveOrZero Integer> seasonNumber = JsonNullable.undefined();
	private JsonNullable<LocalDate> releaseDate = JsonNullable.undefined();
	private JsonNullable<@Positive Integer> runtime = JsonNullable.undefined();
	private JsonNullable<@PositiveOrZero Integer> seasonCount = JsonNullable.undefined();
	private JsonNullable<@PositiveOrZero Integer> episodeCount = JsonNullable.undefined();
	private JsonNullable<@NotBlank @Size(max = 50) String> sportType = JsonNullable.undefined();
	private JsonNullable<Map<String, Object>> metadata = JsonNullable.undefined();
	private JsonNullable<@NotNull List<@NotNull UUID>> genreIds = JsonNullable.undefined();
	private JsonNullable<@NotNull List<@NotBlank @Size(max = 100) String>> manualTags = JsonNullable.undefined();
	private JsonNullable<@NotNull List<@NotNull @Valid ContentCastRequest>> casts = JsonNullable.undefined();

	public void setTitle(JsonNullable<String> title) {
		this.title = map(title, ContentUpdateRequest::strip);
	}

	public void setDescription(JsonNullable<String> description) {
		this.description = requireWrapper(description);
	}

	public void setParentContentId(JsonNullable<UUID> parentContentId) {
		this.parentContentId = requireWrapper(parentContentId);
	}

	public void setSeasonNumber(JsonNullable<Integer> seasonNumber) {
		this.seasonNumber = requireWrapper(seasonNumber);
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

	public void setSportType(JsonNullable<String> sportType) {
		this.sportType = map(sportType, ContentUpdateRequest::strip);
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
