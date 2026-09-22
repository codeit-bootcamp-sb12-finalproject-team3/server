package com.moduplaylist.api.content.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContentCreateResponse {

	private UUID seriesId;
	private List<UUID> contentIds;
	private Instant createdAt;
}
