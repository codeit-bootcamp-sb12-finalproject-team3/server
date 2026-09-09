package com.moduplaylist.api.user.controller;

import com.moduplaylist.api.user.dto.UserCreateRequest;
import com.moduplaylist.api.user.dto.UserResponse;
import com.moduplaylist.api.user.service.UserService;
import com.moduplaylist.api.recommendation.service.RecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
  
  @GetMapping("/me/preferences")
    public ResponseEntity<UserPreferenceResponse> getUserPreferenceContents(
            @AuthenticationPrincipal UUID userId
    ) {
        return ResponseEntity.ok(recommendationService.findUserPreference(userId));
    }
}
