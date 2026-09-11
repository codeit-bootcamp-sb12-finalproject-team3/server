package com.moduplaylist.api.user.controller;

import com.moduplaylist.api.recommendation.service.UserPreferenceService;
import com.moduplaylist.api.recommendation.dto.UserPreferenceCreateRequest;
import com.moduplaylist.api.recommendation.dto.UserPreferenceResponse;
import com.moduplaylist.api.user.dto.UserCreateRequest;
import com.moduplaylist.api.user.dto.UserResponse;
import com.moduplaylist.api.user.dto.UserRoleUpdateRequest;
import com.moduplaylist.api.user.dto.UserLockUpdateRequest;
import com.moduplaylist.api.global.security.CustomUserDetails;
import com.moduplaylist.api.user.service.UserService;
import org.springframework.web.bind.annotation.*;
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
    private final UserPreferenceService userPreferenceService;

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
          @AuthenticationPrincipal CustomUserDetails userDetails
  ) {
    UUID userId = userDetails.getUserId();
    return ResponseEntity.ok(userPreferenceService.findUserPreference(userId));
  }

  @PostMapping("/me/preferences")
  public ResponseEntity<UserPreferenceResponse> createUserPreferenceContents(
          @AuthenticationPrincipal CustomUserDetails userDetails,
          @Valid @RequestBody UserPreferenceCreateRequest request
  ) {
    UUID userId = userDetails.getUserId();
    UserPreferenceResponse response =
            userPreferenceService.createUserPreference(userId, request);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

}
