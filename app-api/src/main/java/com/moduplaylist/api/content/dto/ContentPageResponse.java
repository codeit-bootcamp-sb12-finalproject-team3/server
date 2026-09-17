package com.moduplaylist.api.content.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContentPageResponse {

	@Builder.Default
	private List<ContentSummaryResponse> data = List.of();

	private String cursor;

	private boolean hasNext;
	private long totalCount;
}
