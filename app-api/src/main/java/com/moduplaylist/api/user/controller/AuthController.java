package com.moduplaylist.api.user.controller;

import com.moduplaylist.api.global.security.jwt.JwtDto;
import com.moduplaylist.api.user.dto.CsrfTokenResponse;
import com.moduplaylist.api.user.dto.TokenRefreshResult;
import com.moduplaylist.api.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @Value("${security.jwt.refresh-token-validity-seconds}")
  private long refreshTokenValiditySeconds;

  @Value("${security.jwt.cookie-secure}")
  private boolean cookieSecure;

  @GetMapping("/csrf-token")
  public ResponseEntity<CsrfTokenResponse> getCsrfToken(CsrfToken csrfToken) {
    // 지연된 토큰 로딩을 실행하여 필요하면 CSRF 쿠키를 발급한다.
    String token = csrfToken.getToken();

    return ResponseEntity.ok()
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .body(new CsrfTokenResponse(token));
  }

  @PostMapping("/refresh")
  public ResponseEntity<JwtDto> refresh(
      @CookieValue(name = "REFRESH_TOKEN", required = false) String refreshToken
  ) {
    TokenRefreshResult result = authService.refresh(refreshToken);

    ResponseCookie refreshCookie =
        ResponseCookie.from("REFRESH_TOKEN", result.getRefreshToken())
            .httpOnly(true)
            .secure(cookieSecure)
            .sameSite("Lax")
            .path("/api/auth")
            .maxAge(refreshTokenValiditySeconds)
            .build();

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .body(new JwtDto(result.getUserDto(), result.getAccessToken()));
  }

}