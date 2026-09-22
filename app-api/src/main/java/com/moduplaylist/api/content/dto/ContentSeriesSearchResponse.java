package com.moduplaylist.api.content.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContentSeriesSearchResponse {

	@Builder.Default
	private List<ContentSeriesSuggestionResponse> data = List.of();
}
