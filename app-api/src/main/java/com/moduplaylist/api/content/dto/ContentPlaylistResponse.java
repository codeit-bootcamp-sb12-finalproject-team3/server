package com.moduplaylist.api.content.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContentPlaylistResponse {

	@Builder.Default
	private List<ContentPlaylistItemResponse> data = List.of();

	private boolean hasMore;
}
