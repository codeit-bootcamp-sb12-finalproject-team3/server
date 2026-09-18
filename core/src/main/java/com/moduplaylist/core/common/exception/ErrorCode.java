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
    GENRE_NOT_FOUND("존재하지 않는 장르입니다."),
    CONTENT_NOT_LIKEABLE("좋아요 대상이 아닌 콘텐츠입니다."),
    CONTENT_SEASON_ALREADY_EXISTS("동일한 TV 시리즈에 해당 시즌이 이미 존재합니다."),
    CONTENT_DELETION_BLOCKED("연결된 Watch Party가 있어 콘텐츠를 삭제할 수 없습니다."),
    CONTENT_SEARCH_UNAVAILABLE("콘텐츠 검색 서비스를 사용할 수 없습니다."),
    CONTENT_STORAGE_UNAVAILABLE("콘텐츠 파일 저장소를 사용할 수 없습니다."),

    // Review
    REVIEW_NOT_FOUND("존재하지 않는 리뷰입니다."),
    REVIEW_ALREADY_EXISTS("해당 콘텐츠에 이미 리뷰를 작성했습니다."),
    REVIEW_ACCESS_DENIED("해당 리뷰를 수정하거나 삭제할 권한이 없습니다."),
    CONTENT_NOT_REVIEWABLE("리뷰를 작성할 수 없는 콘텐츠입니다."),

    // Auth
    INVALID_CREDENTIALS("이메일 또는 비밀번호가 올바르지 않습니다."),
    USER_LOCKED("잠긴 계정입니다."),
    FORBIDDEN("요청 권한이 없습니다."),
    INVALID_CSRF_TOKEN("CSRF 토큰이 없거나 올바르지 않습니다."),


    // Playlist
    PLAYLIST_NOT_FOUND("존재하지 않는 플레이리스트입니다."),
    PLAYLIST_ACCESS_DENIED("플레이리스트 수정 또는 삭제 권한이 없습니다."),
    INVALID_PLAYLIST_SEARCH("플레이리스트 검색 조건이 올바르지 않습니다."),
    SELF_PLAYLIST_SUBSCRIPTION_NOT_ALLOWED("본인의 플레이리스트는 구독할 수 없습니다."),
    PLAYLIST_ALREADY_SUBSCRIBED("이미 구독 중인 플레이리스트입니다."),
    PLAYLIST_SUBSCRIPTION_NOT_FOUND("존재하지 않는 플레이리스트 구독 관계입니다."),
    PLAYLIST_CONTENT_ALREADY_EXISTS("이미 플레이리스트에 포함된 콘텐츠입니다."),
    PLAYLIST_CONTENT_NOT_FOUND("플레이리스트에 존재하지 않는 콘텐츠입니다."),
    PLAYLIST_MINIMUM_CONTENT_REQUIRED("플레이리스트에는 최소 4개의 콘텐츠가 필요합니다."),
    INVALID_PLAYLIST_CONTENT_REQUEST("중복된 콘텐츠가 포함되어 있습니다."),

    // Preference
    PREFERENCE_NOT_FOUND("초기 선호 정보가 없습니다."),
    PREFERENCE_ALREADY_EXISTS("이미 초기 선호 정보가 등록되어 있습니다."),
    PREFERENCE_CONTENT_NOT_SELECTABLE("초기 선호로 선택할 수 없는 콘텐츠입니다."),

    // Follow
    SELF_FOLLOW_NOT_ALLOWED("자기 자신을 팔로우할 수 없습니다."),
    FOLLOW_ALREADY_EXISTS("이미 팔로우 중입니다."),
    FOLLOW_NOT_FOUND("존재하지 않는 팔로우 관계입니다."),

    // Notification
    NOTIFICATION_NOT_FOUND("존재하지 않는 알림입니다."),
    NOTIFICATION_ACCESS_DENIED("해당 알림에 접근할 수 없습니다."),

    // Direct Message
    CONVERSATION_NOT_FOUND("존재하지 않는 대화방입니다."),
    CONVERSATION_ACCESS_DENIED("해당 대화방에 접근할 권한이 없습니다."),
    DIRECT_MESSAGE_NOT_FOUND("존재하지 않는 메시지입니다."),
    DIRECT_MESSAGE_CONTENT_INVALID("메시지 내용이 올바르지 않습니다."),
    SELF_DIRECT_MESSAGE_NOT_ALLOWED("자기 자신과 대화방을 만들 수 없습니다."),


    // WatchParty
    WATCHPARTY_NOT_FOUND("존재하지 않는 방입니다."),
    WATCHPARTY_INVALID_EPISODE_RANGE("영화/스포츠 콘텐츠에는 회차 범위를 지정할 수 없습니다."),
    WATCHPARTY_ALREADY_ENDED("이미 종료된 방에는 참가할 수 없습니다."),
    WATCHPARTY_HOST_CANNOT_JOIN("방장은 참가 신청 대상이 아닙니다."),
    WATCHPARTY_ALREADY_JOINED("이미 참가 중입니다."),
    WATCHPARTY_ALREADY_JOINED_ELSEWHERE("이미 다른 Watch Party에 참가 중입니다. 먼저 나가주세요."),
    WATCHPARTY_KICKED_CANNOT_REJOIN("강퇴된 방에는 다시 참가할 수 없습니다."),
    WATCHPARTY_CAPACITY_FULL("정원이 가득 찼습니다."),
    WATCHPARTY_PARTICIPANT_NOT_FOUND("참가 정보를 찾을 수 없습니다."),
    WATCHPARTY_NOT_JOINED("현재 참가 중인 상태가 아닙니다."),
    WATCHPARTY_NOT_A_PARTICIPANT("참가 중인 방이 아닙니다."),
    WATCHPARTY_PARTICIPANT_NOT_JOINED("현재 참가 중인 사용자가 아닙니다.");

    private final String message;
}
