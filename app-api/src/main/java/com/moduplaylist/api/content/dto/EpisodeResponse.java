package com.moduplaylist.api.content.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EpisodeResponse {

	private UUID id;
	private Integer episodeNumber;
	private String title;
	private String description;
	private String thumbnailUrl;
	private Integer runtime;
}
