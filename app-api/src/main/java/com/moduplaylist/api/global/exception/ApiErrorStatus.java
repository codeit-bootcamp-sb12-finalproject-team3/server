package com.moduplaylist.api.global.exception;

import com.moduplaylist.core.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

import java.util.EnumMap;
import java.util.Map;

/**
 * Core의 ErrorCode를 REST API의 HTTP Status로 변환한다.
 * Core가 HTTP에 의존하지 않도록 API 계층에서 상태 코드를 관리한다.

 * 새로운 ErrorCode를 REST API에서 사용하는 경우
 * STATUS_MAP에 해당 ErrorCode와 HttpStatus 매핑을 반드시 추가한다.
 */
public final class ApiErrorStatus {

    private static final Map<ErrorCode, HttpStatus> STATUS_MAP =
            new EnumMap<>(ErrorCode.class);

    static {
        STATUS_MAP.put(ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
        STATUS_MAP.put(ErrorCode.WATCHPARTY_NOT_FOUND, HttpStatus.NOT_FOUND);

        STATUS_MAP.put(ErrorCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND);
        STATUS_MAP.put(ErrorCode.USER_ALREADY_EXISTS, HttpStatus.CONFLICT);

        STATUS_MAP.put(ErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST);
        STATUS_MAP.put(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST);

        STATUS_MAP.put(ErrorCode.SELF_FOLLOW_NOT_ALLOWED, HttpStatus.BAD_REQUEST);
        STATUS_MAP.put(ErrorCode.FOLLOW_ALREADY_EXISTS, HttpStatus.CONFLICT);
        STATUS_MAP.put(ErrorCode.FOLLOW_NOT_FOUND, HttpStatus.NOT_FOUND);

        // Preference
        STATUS_MAP.put(ErrorCode.PREFERENCE_NOT_FOUND, HttpStatus.NOT_FOUND);

        STATUS_MAP.put(
                ErrorCode.INTERNAL_SERVER_ERROR,
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    private ApiErrorStatus() {
    }

    public static HttpStatus from(ErrorCode errorCode) {
        return STATUS_MAP.getOrDefault(
                errorCode,
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}