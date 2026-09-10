package com.moduplaylist.api.global.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 서비스에서 발생한 비즈니스 예외를 공통 API 오류 응답으로 변환한다.

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException e) {
        HttpStatus status = ApiErrorStatus.from(e.getErrorCode());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(e.getTimestamp())
                .code(e.getErrorCode().name())
                .message(e.getMessage())
                .details(e.getData())
                .status(status.value())
                .build();

        return ResponseEntity.status(status).body(response);
    }

    // @Valid 검증 실패 시 필드별 오류 내용을 반환한다.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException e
    ) {
        Map<String, Object> details = new HashMap<>();

        e.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        details.put(error.getField(), error.getDefaultMessage())
                );

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(Instant.now())
                .code(ErrorCode.VALIDATION_ERROR.name())
                .message(ErrorCode.VALIDATION_ERROR.getMessage())
                .details(details)
                .status(HttpStatus.BAD_REQUEST.value())
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    // JSON 형식이 잘못되었거나 필수 요청 본문이 누락된 경우 400 응답으로 처리한다.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(
        HttpMessageNotReadableException e
    ) {
        ErrorResponse response = ErrorResponse.builder()
            .timestamp(Instant.now())
            .code(ErrorCode.INVALID_REQUEST.name())
            .message(ErrorCode.INVALID_REQUEST.getMessage())
            .details(Map.of())
            .status(HttpStatus.BAD_REQUEST.value())
            .build();

        return ResponseEntity.badRequest().body(response);
    }

    // 관리자 권한 등 접근 권한이 부족한 경우 403 응답으로 처리한다.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
        AccessDeniedException e
    ) {
        return handleBaseException(
            new BaseException(ErrorCode.FORBIDDEN, e)
        );
    }

    // 처리되지 않은 예외가 클라이언트에 그대로 노출되지 않도록 500 응답으로 처리한다.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(Instant.now())
                .code(ErrorCode.INTERNAL_SERVER_ERROR.name())
                .message(ErrorCode.INTERNAL_SERVER_ERROR.getMessage())
                .details(Map.of())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .build();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }


}