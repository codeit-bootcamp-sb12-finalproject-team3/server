package com.moduplaylist.api.review.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReviewPageResponse {

	@Builder.Default
	private List<ReviewResponse> data = List.of();

	private Instant nextCursorCreatedAt;
	private UUID nextCursorId;

	private boolean hasNext;
	private long totalCount;
}
