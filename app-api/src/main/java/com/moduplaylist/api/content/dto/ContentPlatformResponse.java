package com.moduplaylist.api.content.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContentPlatformResponse {
	private String regionCode;
	private boolean justWatchAttributionRequired;

	@Builder.Default
	private List<ContentPlatformItemResponse> otts = List.of();
}
