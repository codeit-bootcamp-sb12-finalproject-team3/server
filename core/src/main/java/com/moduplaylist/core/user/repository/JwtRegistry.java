package com.moduplaylist.core.user.repository;

import java.time.Instant;
import java.util.UUID;

public interface JwtRegistry {

  // 새 로그인 정보로 교체하여 기존 로그인 무효화
  void register(
      UUID userId,
      String accessTokenId,
      String refreshTokenId,
      Instant refreshExpiresAt
  );

  boolean isAccessTokenActive(UUID userId, String accessTokenId);

  // 현재 Refresh Token이 일치할 때만 확인과 교체를 하나의 작업으로 처리
  boolean rotate(
      UUID userId,
      String expectedRefreshTokenId,
      String newAccessTokenId,
      String newRefreshTokenId,
      Instant refreshExpiresAt
  );

  // 이전 로그인에서 보낸 로그아웃 요청이 새 로그인을 삭제하지 않도록 함
  // -> 전달된 토큰 ID가 현재 로그인에 속할 때만 해당 로그인 정보를 삭제
  boolean invalidate(UUID userId, String tokenId);

  // 권한 변경 또는 계정 잠금 시 해당 사용자를 강제 로그아웃
  void invalidateByUserId(UUID userId);
}