package com.moduplaylist.api.content.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContentWatchPartyResponse {

	@Builder.Default
	private List<ContentWatchPartyItemResponse> data = List.of();

	private boolean hasMore;
}
