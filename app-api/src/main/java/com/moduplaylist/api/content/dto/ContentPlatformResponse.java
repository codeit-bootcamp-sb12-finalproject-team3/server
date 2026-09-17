package com.moduplaylist.api.content.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContentPlatformResponse {

	@Builder.Default
	private List<ContentPlatformItemResponse> platforms = List.of();
}
