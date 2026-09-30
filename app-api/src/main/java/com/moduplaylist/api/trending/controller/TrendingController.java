package com.moduplaylist.api.trending.controller;

import com.moduplaylist.api.content.dto.ContentSummaryResponse;
import com.moduplaylist.api.global.dto.CursorPageResponse;
import com.moduplaylist.api.global.security.CustomUserDetails;
import com.moduplaylist.api.trending.service.TrendingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/trending")
public class TrendingController {

    private final TrendingService trendingService;

    @GetMapping("/contents")
    public ResponseEntity<CursorPageResponse<ContentSummaryResponse>> getTrendingContents(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                trendingService.findTrendingContents(userDetails.getUserId())
        );
    }
}
