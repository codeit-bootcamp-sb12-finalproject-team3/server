package com.moduplaylist.api.content.dto;

import com.moduplaylist.api.watchparty.dto.WatchPartySummaryResponse;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContentWatchPartyResponse {

	@Builder.Default
	private List<WatchPartySummaryResponse> data = List.of();

	private boolean hasMore;
}
