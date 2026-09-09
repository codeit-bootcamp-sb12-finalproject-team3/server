package com.moduplaylist.api.user.controller;

import com.moduplaylist.api.recommendation.dto.UserPreferenceResponse;
import com.moduplaylist.api.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final RecommendationService recommendationService;

    @GetMapping("/me/preferences")
    public ResponseEntity<UserPreferenceResponse> getUserPreferenceContents(
            @AuthenticationPrincipal UUID userId
    ) {
        return ResponseEntity.ok(recommendationService.findUserPreference(userId));
    }
}
