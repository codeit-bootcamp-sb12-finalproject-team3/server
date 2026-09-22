package com.moduplaylist.api.content.dto;

import com.moduplaylist.api.playlist.dto.PlaylistSummaryResponse;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContentPlaylistResponse {

	@Builder.Default
	private List<PlaylistSummaryResponse> data = List.of();

	private boolean hasMore;
}
