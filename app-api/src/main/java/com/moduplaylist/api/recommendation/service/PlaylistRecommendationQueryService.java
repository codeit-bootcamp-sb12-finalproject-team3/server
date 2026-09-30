package com.moduplaylist.api.recommendation.service;

import com.moduplaylist.api.global.dto.CursorPageResponse;
import com.moduplaylist.api.playlist.dto.PlaylistSummaryResponse;
import java.util.UUID;

public interface PlaylistRecommendationQueryService {

    CursorPageResponse<PlaylistSummaryResponse> findRecommendations(
            UUID userId,
            String cursor,
            int limit
    );
}
