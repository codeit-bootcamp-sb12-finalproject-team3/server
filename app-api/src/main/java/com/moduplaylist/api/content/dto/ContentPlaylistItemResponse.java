package com.moduplaylist.api.content.dto;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContentPlaylistItemResponse {

	private UUID id;
	private String title;
	private String description;
	private long subscriberCount;
	private BigDecimal weeklyPopularityScore;
	private Instant createdAt;
}
