package com.moduplaylist.api.content.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContentOttResponse {

	@Builder.Default
	private List<ContentOttItemResponse> otts = List.of();
}
