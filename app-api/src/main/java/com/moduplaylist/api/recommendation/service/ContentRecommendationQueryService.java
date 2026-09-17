package com.moduplaylist.api.recommendation.service;

import com.moduplaylist.api.content.dto.ContentSummaryResponse;
import com.moduplaylist.api.global.dto.CursorPageResponse;
import java.util.UUID;

public interface ContentRecommendationQueryService {

    CursorPageResponse<ContentSummaryResponse> findRecommendations(
            UUID userId,
            String cursor,
            UUID idAfter,
            int limit
    );
}
