package com.moduplaylist.core.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_REQUEST("잘못된 요청입니다."),
    VALIDATION_ERROR("요청 데이터 유효성 검사에 실패했습니다."),
    INTERNAL_SERVER_ERROR("서버 내부 오류가 발생했습니다."),

    // User
    USER_NOT_FOUND("존재하지 않는 사용자입니다."),
    USER_ALREADY_EXISTS("이미 가입된 사용자입니다.");

    // 이후 실제 기능 개발하며 추가

    private final String message;
}