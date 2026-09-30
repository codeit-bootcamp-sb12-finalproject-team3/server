package com.moduplaylist.api.content.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.openapitools.jackson.nullable.JsonNullable;

@Getter
@NoArgsConstructor
public class EpisodeUpdateRequest {

	private JsonNullable<@NotNull @PositiveOrZero Integer> episodeNumber =
		JsonNullable.undefined();
	private JsonNullable<@Size(max = 255) String> title =
		JsonNullable.undefined();
	private JsonNullable<String> description = JsonNullable.undefined();
	private JsonNullable<@Positive Integer> runtime = JsonNullable.undefined();
	private boolean removeThumbnail;
	private final Set<String> unknownFields = new HashSet<>();

	public void setEpisodeNumber(JsonNullable<Integer> episodeNumber) {
		this.episodeNumber = requireWrapper(episodeNumber);
	}

	public void setTitle(JsonNullable<String> title) {
		this.title = map(title, EpisodeUpdateRequest::strip);
	}

	public void setDescription(JsonNullable<String> description) {
		this.description = map(description, EpisodeUpdateRequest::strip);
	}

	public void setRuntime(JsonNullable<Integer> runtime) {
		this.runtime = requireWrapper(runtime);
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
