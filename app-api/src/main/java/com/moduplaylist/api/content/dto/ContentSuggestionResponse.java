package com.moduplaylist.api.content.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContentSuggestionResponse {

	private UUID contentId;
	private String title;
	private ContentSummaryType type;
	private String thumbnailUrl;
	private String matchedText;
}
