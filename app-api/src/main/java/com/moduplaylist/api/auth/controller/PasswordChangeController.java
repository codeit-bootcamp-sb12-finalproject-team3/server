package com.moduplaylist.api.auth.controller;

import com.moduplaylist.api.auth.dto.PasswordChangeRequest;
import com.moduplaylist.api.auth.service.PasswordChangeService;
import com.moduplaylist.api.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class PasswordChangeController {

  private final PasswordChangeService passwordChangeService;

  @PatchMapping("/password")
  public ResponseEntity<Void> changePassword(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody PasswordChangeRequest request
  ) {
    passwordChangeService.change(userDetails.getUserId(), request);
    return ResponseEntity.noContent().build();
  }
}