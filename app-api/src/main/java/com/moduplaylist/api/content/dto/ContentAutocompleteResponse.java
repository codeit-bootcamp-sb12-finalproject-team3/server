package com.moduplaylist.api.content.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContentAutocompleteResponse {

	@Builder.Default
	private List<ContentSuggestionResponse> suggestions = List.of();
}
