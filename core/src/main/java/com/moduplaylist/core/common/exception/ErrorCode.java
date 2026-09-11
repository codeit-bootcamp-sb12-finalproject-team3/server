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
    USER_ALREADY_EXISTS("이미 가입된 사용자입니다."),
    UNAUTHORIZED("인증이 필요합니다."),

    // Content
    CONTENT_NOT_FOUND("존재하지 않는 콘텐츠입니다."),
  
    // Auth
    INVALID_CREDENTIALS("이메일 또는 비밀번호가 올바르지 않습니다."),
    USER_LOCKED("잠긴 계정입니다."),
    FORBIDDEN("요청 권한이 없습니다."),
    INVALID_CSRF_TOKEN("CSRF 토큰이 없거나 올바르지 않습니다."),

    // Preference
    PREFERENCE_NOT_FOUND("초기 선호 정보가 없습니다."),
    PREFERENCE_ALREADY_EXISTS("이미 초기 선호 정보가 등록되어 있습니다."),

    // Follow
    SELF_FOLLOW_NOT_ALLOWED("자기 자신을 팔로우할 수 없습니다."),
    FOLLOW_ALREADY_EXISTS("이미 팔로우 중입니다."),
    FOLLOW_NOT_FOUND("존재하지 않는 팔로우 관계입니다."),

    // WatchParty
    WATCHPARTY_NOT_FOUND("존재하지 않는 방입니다."),
    WATCHPARTY_ALREADY_ENDED("이미 종료된 방에는 참가할 수 없습니다."),
    WATCHPARTY_HOST_CANNOT_JOIN("방장은 참가 신청 대상이 아닙니다."),
    WATCHPARTY_ALREADY_JOINED("이미 참가 중입니다."),
    WATCHPARTY_KICKED_CANNOT_REJOIN("강퇴된 방에는 다시 참가할 수 없습니다."),
    WATCHPARTY_CAPACITY_FULL("정원이 가득 찼습니다."),
    WATCHPARTY_PARTICIPANT_NOT_FOUND("참가 정보를 찾을 수 없습니다."),
    WATCHPARTY_NOT_JOINED("현재 참가 중인 상태가 아닙니다."),
    WATCHPARTY_NOT_A_PARTICIPANT("참가 중인 방이 아닙니다."),
    WATCHPARTY_PARTICIPANT_NOT_JOINED("현재 참가 중인 사용자가 아닙니다.");

    private final String message;
}
