package com.moduplaylist.api.recommendation.controller;

import com.moduplaylist.api.content.dto.ContentSummaryResponse;
import com.moduplaylist.api.global.dto.CursorPageResponse;
import com.moduplaylist.api.global.security.CustomUserDetails;
import com.moduplaylist.api.playlist.dto.PlaylistSummaryResponse;
import com.moduplaylist.api.recommendation.service.ContentRecommendationQueryService;
import com.moduplaylist.api.recommendation.service.PlaylistRecommendationQueryService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final ContentRecommendationQueryService recommendationQueryService;
    private final PlaylistRecommendationQueryService playlistRecommendationQueryService;

    @GetMapping("/contents")
    public ResponseEntity<CursorPageResponse<ContentSummaryResponse>> getContentRecommendations(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit
    ) {
        CursorPageResponse<ContentSummaryResponse> response =
                recommendationQueryService.findRecommendations(
                        userDetails.getUserId(),
                        cursor,
                        limit
                );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/playlists")
    public ResponseEntity<CursorPageResponse<PlaylistSummaryResponse>> getPlaylistRecommendations(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit
    ) {
        CursorPageResponse<PlaylistSummaryResponse> response =
                playlistRecommendationQueryService.findRecommendations(
                        userDetails.getUserId(),
                        cursor,
                        limit
                );
        return ResponseEntity.ok(response);
    }
}
