package com.moduplaylist.api.content.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TvSeasonDetail {

	private UUID parentContentId;
	private String seriesTitle;
	private Integer seasonNumber;
	private Integer episodeCount;
	private int registeredEpisodeCount;
}
