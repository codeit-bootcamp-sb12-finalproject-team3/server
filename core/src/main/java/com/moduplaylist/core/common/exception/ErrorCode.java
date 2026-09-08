package com.moduplaylist.core.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_REQUEST("잘못된 요청입니다."),
    ACCESS_DENIED("요청한 작업을 수행할 권한이 없습니다."),
    AUTHENTICATION_REQUIRED("유효한 사용자 인증이 필요합니다."),
    VALIDATION_ERROR("요청 데이터 유효성 검사에 실패했습니다."),
    INTERNAL_SERVER_ERROR("서버 내부 오류가 발생했습니다."),

    // User
    USER_NOT_FOUND("존재하지 않는 사용자입니다."),
    USER_ALREADY_EXISTS("이미 가입된 사용자입니다."),

    // Content
    CONTENT_NOT_FOUND("존재하지 않는 콘텐츠입니다."),
    CONTENT_CONFLICT("콘텐츠의 중복 또는 연결 관계로 인해 요청을 처리할 수 없습니다."),
    CONTENT_DELETE_RESTRICTED("Watch Party가 연결된 콘텐츠는 삭제할 수 없습니다."),

    // Review
    REVIEW_NOT_FOUND("존재하지 않는 리뷰입니다."),
    REVIEW_ALREADY_EXISTS("이미 해당 콘텐츠에 리뷰를 작성했습니다."),
    REVIEW_ACCESS_DENIED("본인이 작성한 리뷰만 수정하거나 삭제할 수 있습니다."),

    // Preference
    PREFERENCE_NOT_FOUND( "초기 선호 정보가 없습니다.");



    private final String message;
}
