package com.moduplaylist.api.content.service;

import com.moduplaylist.api.content.dto.ContentSummaryResponse;
import com.moduplaylist.api.global.dto.CursorPageResponse;
import java.util.UUID;

public interface NewContentService {

	CursorPageResponse<ContentSummaryResponse> findNewContents(
		UUID userId,
		String cursor,
		UUID idAfter,
		int limit
	);
}
