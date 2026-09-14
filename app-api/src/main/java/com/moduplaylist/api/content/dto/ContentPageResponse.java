package com.moduplaylist.api.content.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContentPageResponse {

	@Builder.Default
	private List<ContentSummaryResponse> data = List.of();

	private Instant nextCursorCreatedAt;
	private Instant nextCursorLikedAt;
	private BigDecimal nextCursorRating;
	private UUID nextCursorId;

	private boolean hasNext;
	private long totalCount;
}
