package com.moduplaylist.infrastructure.redis.jwt;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class JwtInformation {

  private String accessTokenId;
  private String refreshTokenId;
}