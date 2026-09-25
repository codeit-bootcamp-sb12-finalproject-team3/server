package com.moduplaylist.api.trending.service;

import com.moduplaylist.api.content.dto.ContentSummaryResponse;
import com.moduplaylist.api.global.dto.CursorPageResponse;
import java.util.UUID;

public interface TrendingService {

    CursorPageResponse<ContentSummaryResponse> findTrendingContents(UUID userId);
}
