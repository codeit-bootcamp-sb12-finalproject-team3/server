package com.moduplaylist.api.auth.oauth;

import com.moduplaylist.core.user.repository.OAuth2AuthorizationRequestStore;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;

@Component
public class RedisOAuth2AuthorizationRequestRepository
    implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

  private static final String COOKIE_NAME = "MOPL_OAUTH2_REQUEST";
  private static final String COOKIE_PATH = "/login/oauth2/code";
  private static final Duration TTL = Duration.ofMinutes(3);

  private final OAuth2AuthorizationRequestStore requestStore;
  private final SecureRandom secureRandom = new SecureRandom();
  private final boolean cookieSecure;

  public RedisOAuth2AuthorizationRequestRepository(
      OAuth2AuthorizationRequestStore requestStore,
      @Value("${security.jwt.cookie-secure}") boolean cookieSecure
  ) {
    this.requestStore = requestStore;
    this.cookieSecure = cookieSecure;
  }

  @Override
  public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
    String state = request.getParameter("state");
    String browserToken = readCookie(request);

    if (state == null || state.isBlank() || browserToken == null) {
      return null;
    }

    String serializedRequest = requestStore.find(state, browserToken);
    return deserialize(serializedRequest);
  }

  @Override
  public void saveAuthorizationRequest(
      OAuth2AuthorizationRequest authorizationRequest,
      HttpServletRequest request,
      HttpServletResponse response
  ) {
    if (authorizationRequest == null) {
      removeAuthorizationRequest(request, response);
      return;
    }

    String browserToken = generateBrowserToken();
    String serializedRequest = Base64.getEncoder().encodeToString(
        SerializationUtils.serialize(authorizationRequest)
    );

    requestStore.save(authorizationRequest.getState(), browserToken, serializedRequest, TTL);
    writeCookie(response, browserToken, TTL);
  }

  @Override
  public OAuth2AuthorizationRequest removeAuthorizationRequest(
      HttpServletRequest request,
      HttpServletResponse response
  ) {
    String state = request.getParameter("state");
    String browserToken = readCookie(request);

    clearCookie(response);

    if (state == null || state.isBlank() || browserToken == null) {
      return null;
    }

    String serializedRequest = requestStore.consume(state, browserToken);
    return deserialize(serializedRequest);
  }

  private OAuth2AuthorizationRequest deserialize(String serializedRequest) {
    if (serializedRequest == null) {
      return null;
    }

    byte[] bytes = Base64.getDecoder().decode(serializedRequest);
    Object value = SerializationUtils.deserialize(bytes);

    return value instanceof OAuth2AuthorizationRequest authorizationRequest
        ? authorizationRequest
        : null;
  }

  private String readCookie(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();

    if (cookies == null) {
      return null;
    }

    return Arrays.stream(cookies)
        .filter(cookie -> COOKIE_NAME.equals(cookie.getName()))
        .map(Cookie::getValue)
        .filter(value -> !value.isBlank())
        .findFirst()
        .orElse(null);
  }

  private String generateBrowserToken() {
    byte[] bytes = new byte[32];
    secureRandom.nextBytes(bytes);

    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private void writeCookie(HttpServletResponse response, String value, Duration maxAge) {
    ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, value)
        .httpOnly(true)
        .secure(cookieSecure)
        .sameSite("Lax")
        .path(COOKIE_PATH)
        .maxAge(maxAge)
        .build();

    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
  }

  private void clearCookie(HttpServletResponse response) {
    writeCookie(response, "", Duration.ZERO);
  }
}
