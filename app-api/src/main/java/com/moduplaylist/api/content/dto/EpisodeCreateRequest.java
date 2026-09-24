package com.moduplaylist.api.content.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EpisodeCreateRequest {

	@NotNull
	@PositiveOrZero
	private Integer episodeNumber;

	@Size(max = 255)
	private String title;

	private String description;

	@Positive
	private Integer runtime;

	public void setEpisodeNumber(Integer episodeNumber) { this.episodeNumber = episodeNumber; }
	public void setTitle(String title) { this.title = strip(title); }
	public void setDescription(String description) { this.description = strip(description); }
	public void setRuntime(Integer runtime) { this.runtime = runtime; }

	private static String strip(String value) {
		return value == null ? null : value.strip();
	}
}
