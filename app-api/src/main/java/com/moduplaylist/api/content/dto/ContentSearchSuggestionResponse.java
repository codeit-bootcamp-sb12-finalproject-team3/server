package com.moduplaylist.api.content.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContentSearchSuggestionResponse {

	private String text;
	private String type;
}
