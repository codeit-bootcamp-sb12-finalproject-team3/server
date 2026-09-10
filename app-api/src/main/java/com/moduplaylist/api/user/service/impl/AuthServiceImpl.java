package com.moduplaylist.api.user.service.impl;

import com.moduplaylist.api.global.security.CustomUserDetails;
import com.moduplaylist.api.global.security.CustomUserDetailsService;
import com.moduplaylist.api.global.security.jwt.JwtTokenProvider;
import com.moduplaylist.api.user.dto.TokenRefreshResult;
import com.moduplaylist.api.user.service.AuthService;
import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import com.moduplaylist.core.user.repository.JwtRegistry;
import com.nimbusds.jwt.JWTClaimsSet;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  private final JwtTokenProvider jwtTokenProvider;
  private final JwtRegistry jwtRegistry;
  private final CustomUserDetailsService customUserDetailsService;

  private final AccountStatusUserDetailsChecker userDetailsChecker =
      new AccountStatusUserDetailsChecker();

  @Override
  public TokenRefreshResult refresh(String refreshToken) {
    try {
      JWTClaimsSet refreshClaims =
          jwtTokenProvider.validateRefreshToken(refreshToken);

      UUID userId = UUID.fromString(refreshClaims.getSubject());

      CustomUserDetails userDetails =
          (CustomUserDetails) customUserDetailsService.loadUserById(userId);

      // 로그인 이후 계정이 잠겼다면 토큰 갱신도 허용하지 않는다.
      userDetailsChecker.check(userDetails);

      String newAccessToken = jwtTokenProvider.generateAccessToken(userId);
      String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId);

      JWTClaimsSet newAccessClaims =
          jwtTokenProvider.validateAccessToken(newAccessToken);
      JWTClaimsSet newRefreshClaims =
          jwtTokenProvider.validateRefreshToken(newRefreshToken);

      // 현재 저장된 Refresh Token과 일치할 때만 새 토큰으로 교체한다.
      boolean rotated = jwtRegistry.rotate(
          userId,
          refreshClaims.getJWTID(),
          newAccessClaims.getJWTID(),
          newRefreshClaims.getJWTID(),
          newRefreshClaims.getExpirationTime().toInstant()
      );

      if (!rotated) {
        throw new BaseException(ErrorCode.UNAUTHORIZED);
      }

      return new TokenRefreshResult(
          userDetails.getUserResponse(),
          newAccessToken,
          newRefreshToken
      );

    } catch (LockedException e) {
      throw new BaseException(ErrorCode.USER_LOCKED, e);
    } catch (AuthenticationException e) {
      throw new BaseException(ErrorCode.UNAUTHORIZED, e);
    }
  }

  @Override
  public void logout(String refreshToken) {
    if (refreshToken == null || refreshToken.isBlank()) {
      return;
    }

    JWTClaimsSet claims;

    try {
      claims = jwtTokenProvider.validateRefreshToken(refreshToken);
    } catch (AuthenticationException e) {
      // 유효하지 않은 토큰은 Redis 삭제에 사용하지 않는다.
      return;
    }

    UUID userId = UUID.fromString(claims.getSubject());

    // 이전 로그인에서 보낸 로그아웃 요청이 새 로그인을 삭제하지 않도록 한다.
    jwtRegistry.invalidate(userId, claims.getJWTID());
  }
}