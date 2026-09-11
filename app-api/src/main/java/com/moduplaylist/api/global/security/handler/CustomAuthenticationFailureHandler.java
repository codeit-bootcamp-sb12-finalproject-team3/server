package com.moduplaylist.api.global.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moduplaylist.api.global.exception.ApiErrorStatus;
import com.moduplaylist.api.global.exception.ErrorResponse;
import com.moduplaylist.core.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationFailureHandler
    implements AuthenticationFailureHandler {

  private final ObjectMapper objectMapper;

  @Override
  public void onAuthenticationFailure(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException exception
  ) throws IOException {

    ErrorCode errorCode;

    if (exception instanceof LockedException) {
      errorCode = ErrorCode.USER_LOCKED;
    } else if (exception instanceof BadCredentialsException
        || exception instanceof UsernameNotFoundException) {
      errorCode = ErrorCode.INVALID_CREDENTIALS;
    } else {
      errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
    }

    HttpStatus status = ApiErrorStatus.from(errorCode);

    ErrorResponse errorResponse = ErrorResponse.builder()
        .timestamp(Instant.now())
        .code(errorCode.name())
        .message(errorCode.getMessage())
        .details(Map.of())
        .status(status.value())
        .build();

    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());

    objectMapper.writeValue(response.getWriter(), errorResponse);
  }
}