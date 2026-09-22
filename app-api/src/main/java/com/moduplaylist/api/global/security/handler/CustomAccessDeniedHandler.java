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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

  private final ObjectMapper objectMapper;

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException exception
  ) throws IOException {

    ErrorCode errorCode = exception instanceof CsrfException
        ? ErrorCode.INVALID_CSRF_TOKEN
        : ErrorCode.FORBIDDEN;

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
