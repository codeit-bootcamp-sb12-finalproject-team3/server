package com.moduplaylist.api.auth.controller;

import com.moduplaylist.api.auth.dto.CsrfTokenResponse;
import com.moduplaylist.api.auth.dto.OAuth2CodeExchangeRequest;
import com.moduplaylist.api.auth.dto.TemporaryPasswordIssueRequest;
import com.moduplaylist.api.auth.dto.TokenRefreshResult;
import com.moduplaylist.api.auth.service.AuthService;
import com.moduplaylist.api.auth.service.OAuth2CodeExchangeService;
import com.moduplaylist.api.auth.service.TemporaryPasswordService;
import com.moduplaylist.api.global.security.jwt.JwtDto;
import com.moduplaylist.core.user.exception.InvalidOAuth2LoginCodeException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final OAuth2CodeExchangeService oAuth2CodeExchangeService;
  private final TemporaryPasswordService temporaryPasswordService;

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

  @PostMapping("/oauth/exchange")
  public ResponseEntity<JwtDto> exchangeOAuth2Code(
      @RequestBody OAuth2CodeExchangeRequest request,
      @CookieValue(name = "MOPL_OAUTH2_EXCHANGE", required = false) String browserToken
  ) {
    if (request == null) {
      throw new InvalidOAuth2LoginCodeException();
    }

    TokenRefreshResult result = oAuth2CodeExchangeService.exchange(request.getCode(), browserToken);

    ResponseCookie refreshCookie = ResponseCookie.from("REFRESH_TOKEN", result.getRefreshToken())
        .httpOnly(true)
        .secure(cookieSecure)
        .sameSite("Lax")
        .path("/api/auth")
        .maxAge(refreshTokenValiditySeconds)
        .build();

    ResponseCookie exchangeCookie = ResponseCookie.from("MOPL_OAUTH2_EXCHANGE", "")
        .httpOnly(true)
        .secure(cookieSecure)
        .sameSite("Lax")
        .path("/api/auth/oauth")
        .maxAge(0)
        .build();

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, refreshCookie.toString(), exchangeCookie.toString())
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .body(new JwtDto(result.getUserDto(), result.getAccessToken()));
  }

  @PostMapping("/refresh")
  public ResponseEntity<JwtDto> refresh(
      @CookieValue(name = "REFRESH_TOKEN", required = false) String refreshToken
  ) {
    TokenRefreshResult result = authService.refresh(refreshToken);

    ResponseCookie refreshCookie = ResponseCookie.from("REFRESH_TOKEN", result.getRefreshToken())
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

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(
      @CookieValue(name = "REFRESH_TOKEN", required = false) String refreshToken
  ) {
    authService.logout(refreshToken);

    ResponseCookie refreshCookie = ResponseCookie.from("REFRESH_TOKEN", "")
        .httpOnly(true)
        .secure(cookieSecure)
        .sameSite("Lax")
        .path("/api/auth")
        .maxAge(0)
        .build();

    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .build();
  }

  @PostMapping("/password/reset-request")
  public ResponseEntity<Void> requestPasswordReset(
      @Valid @RequestBody TemporaryPasswordIssueRequest request
  ) {
    temporaryPasswordService.issue(request.email());
    return ResponseEntity.noContent().build();
  }
}