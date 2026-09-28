
package com.moduplaylist.api.global.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moduplaylist.api.auth.dto.TokenRefreshResult;
import com.moduplaylist.api.auth.service.LoginTokenIssuer;
import com.moduplaylist.api.global.security.CustomUserDetails;
import com.moduplaylist.api.global.security.jwt.JwtDto;
import com.moduplaylist.api.user.dto.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

  private final ObjectMapper objectMapper;
  private final LoginTokenIssuer loginTokenIssuer;

  @Value("${security.jwt.refresh-token-validity-seconds}")
  private long refreshTokenValiditySeconds;

  @Value("${security.jwt.cookie-secure}")
  private boolean cookieSecure;

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request,
      HttpServletResponse response,
      Authentication authentication
  ) throws IOException {
    CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
    UserResponse userResponse = userDetails.getUserResponse();

    TokenRefreshResult tokens = loginTokenIssuer.issue(userResponse);

    ResponseCookie refreshCookie = ResponseCookie
        .from("REFRESH_TOKEN", tokens.getRefreshToken())
        .httpOnly(true)
        .secure(cookieSecure)
        .sameSite("Lax")
        .path("/api/auth")
        .maxAge(refreshTokenValiditySeconds)
        .build();

    response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());

    JwtDto jwtDto = new JwtDto(tokens.getUserDto(), tokens.getAccessToken());
    objectMapper.writeValue(response.getWriter(), jwtDto);
  }
}
