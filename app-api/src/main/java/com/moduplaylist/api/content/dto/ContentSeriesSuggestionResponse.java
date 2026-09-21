package com.moduplaylist.api.content.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContentSeriesSuggestionResponse {

	private UUID id;
	private String title;
	private String originalTitle;
}
