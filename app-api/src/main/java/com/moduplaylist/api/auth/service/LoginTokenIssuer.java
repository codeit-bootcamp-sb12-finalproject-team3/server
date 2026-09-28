package com.moduplaylist.api.auth.service;

import com.moduplaylist.api.auth.dto.TokenRefreshResult;
import com.moduplaylist.api.global.security.jwt.JwtTokenProvider;
import com.moduplaylist.api.user.dto.UserResponse;
import com.moduplaylist.core.user.repository.JwtRegistry;
import com.nimbusds.jwt.JWTClaimsSet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginTokenIssuer {

  private final JwtTokenProvider jwtTokenProvider;
  private final JwtRegistry jwtRegistry;

  public TokenRefreshResult issue(UserResponse userResponse) {
    String accessToken = jwtTokenProvider.generateAccessToken(userResponse.getId());
    String refreshToken = jwtTokenProvider.generateRefreshToken(userResponse.getId());

    JWTClaimsSet accessClaims = jwtTokenProvider.validateAccessToken(accessToken);
    JWTClaimsSet refershClaims = jwtTokenProvider.validateRefreshToken(refreshToken);

    // 신규 로그인 등록 시 기존 로그인 정보를 무효화한다.
    jwtRegistry.register(
        userResponse.getId(),
        accessClaims.getJWTID(),
        refershClaims.getJWTID(),
        refershClaims.getExpirationTime().toInstant()
    );

    return new TokenRefreshResult(userResponse, accessToken, refreshToken);
  }
}
