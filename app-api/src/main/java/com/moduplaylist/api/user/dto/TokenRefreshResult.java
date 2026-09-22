package com.moduplaylist.api.user.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class TokenRefreshResult {

  private final UserResponse userDto;
  private final String accessToken;
  private final String refreshToken;
}