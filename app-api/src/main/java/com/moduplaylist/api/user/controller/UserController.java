package com.moduplaylist.api.user.controller;

import com.moduplaylist.api.recommendation.service.UserPreferenceService;
import com.moduplaylist.api.recommendation.dto.UserPreferenceCreateRequest;
import com.moduplaylist.api.recommendation.dto.UserPreferenceResponse;
import com.moduplaylist.api.user.dto.UserCreateRequest;
import com.moduplaylist.api.user.dto.UserResponse;
import com.moduplaylist.api.user.service.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserPreferenceService userPreferenceService;

    @PostMapping
    public ResponseEntity<UserResponse> create(
            @Valid @RequestBody UserCreateRequest request
    ) {
        UserResponse response = userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me/preferences")
    public ResponseEntity<UserPreferenceResponse> getUserPreferenceContents(
            @AuthenticationPrincipal UUID userId
    ) {
        return ResponseEntity.ok(userPreferenceService.findUserPreference(userId));
    }

    @PostMapping("/me/preferences")
    public ResponseEntity<UserPreferenceResponse> createUserPreferenceContents(
            @RequestParam UUID userId,
            @Valid @RequestBody UserPreferenceCreateRequest request
    ) {
        UserPreferenceResponse response =
                userPreferenceService.createUserPreference(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
