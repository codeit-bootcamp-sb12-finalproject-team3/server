package com.moduplaylist.api.user.controller;

import com.moduplaylist.api.recommendation.service.RecommendationService;
import com.moduplaylist.api.recommendation.dto.UserPreferenceCreateRequest;
import com.moduplaylist.api.recommendation.dto.UserPreferenceResponse;
import com.moduplaylist.api.user.dto.UserCreateRequest;
import com.moduplaylist.api.user.dto.UserResponse;
import com.moduplaylist.api.user.dto.UserRoleUpdateRequest;
import com.moduplaylist.api.user.dto.UserLockUpdateRequest;
import com.moduplaylist.api.user.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RecommendationService recommendationService;

  @PostMapping
  public ResponseEntity<UserResponse> create(
      @Valid @RequestBody UserCreateRequest request
  ) {
    UserResponse response = userService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PatchMapping("/{userId}/role")
  public ResponseEntity<UserResponse> updateRole(
      @PathVariable("userId") UUID userId,
      @Valid @RequestBody UserRoleUpdateRequest request
  ) {
    UserResponse response =
        userService.updateRole(userId, request.getRole());

    return ResponseEntity.ok(response);
  }

  @PatchMapping("/{userId}/locked")
  public ResponseEntity<UserResponse> updateLocked(
      @PathVariable("userId") UUID userId,
      @Valid @RequestBody UserLockUpdateRequest request
  ) {
    UserResponse response =
        userService.updateLocked(userId, request.getLocked());

    return ResponseEntity.ok(response);
  }
  
    @GetMapping("/me/preferences")
    public ResponseEntity<UserPreferenceResponse> getUserPreferenceContents(
            @AuthenticationPrincipal UUID userId
    ) {
        return ResponseEntity.ok(recommendationService.findUserPreference(userId));
    }

    @PostMapping("/me/preferences")
    public ResponseEntity<UserPreferenceResponse> createUserPreferenceContents(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody UserPreferenceCreateRequest request
    ) {
        UserPreferenceResponse response =
                recommendationService.createUserPreference(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
