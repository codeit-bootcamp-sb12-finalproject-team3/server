package com.moduplaylist.api.auth.oauth;

import com.moduplaylist.api.auth.service.OAuth2UserService;
import com.moduplaylist.api.user.dto.UserResponse;
import com.moduplaylist.core.user.repository.OAuth2LoginCodeStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

  private static final String COOKIE_NAME = "MOPL_OAUTH2_EXCHANGE";
  private static final String COOKIE_PATH = "/api/auth/oauth";
  private static final Duration CODE_TTL = Duration.ofMinutes(3);

  private final OAuth2UserService oAuth2UserService;
  private final OAuth2LoginCodeStore loginCodeStore;
  private final SecureRandom secureRandom = new SecureRandom();

  @Value("${security.jwt.cookie-secure}")
  private boolean cookieSecure;

  @Value("${mopl.oauth2.frontend-callback-url}")
  private String frontendCallbackUrl;

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request,
      HttpServletResponse response,
      Authentication authentication
  ) throws IOException {
    OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;

    UserResponse user = oAuth2UserService.findOrCreate(
        oauthToken.getAuthorizedClientRegistrationId(),
        oauthToken.getPrincipal()
    );

    String code = generateToken();
    String browserToken = generateToken();

    loginCodeStore.save(code, browserToken, user.getId().toString(), CODE_TTL);

    ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, browserToken)
        .httpOnly(true)
        .secure(cookieSecure)
        .sameSite("Lax")
        .path(COOKIE_PATH)
        .maxAge(CODE_TTL)
        .build();

    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
    response.sendRedirect(frontendCallbackUrl + "?code=" + code);
  }

  private String generateToken() {
    byte[] bytes = new byte[32];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
