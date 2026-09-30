package com.moduplaylist.api.global.security.jwt;

import com.moduplaylist.api.user.dto.UserResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class JwtDto {

  private final UserResponse userDto;
  private final String accessToken;

}
