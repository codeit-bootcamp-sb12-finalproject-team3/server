package com.moduplaylist.api.global.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * HTTP 요청마다 추적용 MDC 정보를 설정한다.
 * 동일 요청에서 발생한 로그를 requestId 기준으로 추적할 수 있다.
 */
@Slf4j
public class MDCLoggingInterceptor implements HandlerInterceptor {

    public static final String REQUEST_ID = "requestId";
    public static final String REQUEST_METHOD = "requestMethod";
    public static final String REQUEST_URI = "requestUri";
    public static final String REQUEST_ID_HEADER = "X-Request-ID";

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {
        // IP는 따로 추가하지 않음
        String requestId = UUID.randomUUID().toString();

        MDC.put(REQUEST_ID, requestId);
        MDC.put(REQUEST_METHOD, request.getMethod());
        MDC.put(REQUEST_URI, request.getRequestURI());

        response.setHeader(REQUEST_ID_HEADER, requestId);

        log.debug("Request started");

        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {
        log.debug("Request finished");

        MDC.clear();
    }
}