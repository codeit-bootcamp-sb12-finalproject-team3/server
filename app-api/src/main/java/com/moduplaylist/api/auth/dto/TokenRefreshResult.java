package com.moduplaylist.api.auth.dto;

import com.moduplaylist.api.user.dto.UserResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class TokenRefreshResult {

  private final UserResponse userDto;
  private final String accessToken;
  private final String refreshToken;
}