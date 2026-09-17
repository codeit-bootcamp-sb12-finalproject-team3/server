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

        STATUS_MAP.put(ErrorCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND);
        STATUS_MAP.put(ErrorCode.USER_ALREADY_EXISTS, HttpStatus.CONFLICT);

        STATUS_MAP.put(ErrorCode.CONTENT_NOT_FOUND, HttpStatus.NOT_FOUND);
        STATUS_MAP.put(ErrorCode.GENRE_NOT_FOUND, HttpStatus.NOT_FOUND);
        STATUS_MAP.put(ErrorCode.CONTENT_NOT_LIKEABLE, HttpStatus.BAD_REQUEST);
        STATUS_MAP.put(ErrorCode.CONTENT_SEASON_ALREADY_EXISTS, HttpStatus.CONFLICT);
        STATUS_MAP.put(ErrorCode.CONTENT_DELETION_BLOCKED, HttpStatus.CONFLICT);
        STATUS_MAP.put(ErrorCode.CONTENT_SEARCH_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE);
        STATUS_MAP.put(ErrorCode.CONTENT_STORAGE_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE);

        // Review
        STATUS_MAP.put(ErrorCode.REVIEW_NOT_FOUND, HttpStatus.NOT_FOUND);
        STATUS_MAP.put(ErrorCode.REVIEW_ALREADY_EXISTS, HttpStatus.CONFLICT);
        STATUS_MAP.put(ErrorCode.REVIEW_ACCESS_DENIED, HttpStatus.FORBIDDEN);
        STATUS_MAP.put(ErrorCode.CONTENT_NOT_REVIEWABLE, HttpStatus.BAD_REQUEST);

        // Auth
        STATUS_MAP.put(ErrorCode.INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED);
        STATUS_MAP.put(ErrorCode.USER_LOCKED, HttpStatus.UNAUTHORIZED);
        STATUS_MAP.put(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        STATUS_MAP.put(ErrorCode.INVALID_CSRF_TOKEN, HttpStatus.FORBIDDEN);

        STATUS_MAP.put(ErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST);
        STATUS_MAP.put(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST);

        // Playlist
        STATUS_MAP.put(ErrorCode.PLAYLIST_NOT_FOUND, HttpStatus.NOT_FOUND);
        STATUS_MAP.put(ErrorCode.PLAYLIST_ACCESS_DENIED, HttpStatus.FORBIDDEN);
        STATUS_MAP.put(ErrorCode.INVALID_PLAYLIST_SEARCH, HttpStatus.BAD_REQUEST);
        STATUS_MAP.put(ErrorCode.SELF_PLAYLIST_SUBSCRIPTION_NOT_ALLOWED, HttpStatus.BAD_REQUEST);
        STATUS_MAP.put(ErrorCode.PLAYLIST_ALREADY_SUBSCRIBED, HttpStatus.CONFLICT);
        STATUS_MAP.put(ErrorCode.PLAYLIST_SUBSCRIPTION_NOT_FOUND, HttpStatus.NOT_FOUND);
        STATUS_MAP.put(ErrorCode.PLAYLIST_CONTENT_ALREADY_EXISTS, HttpStatus.CONFLICT);
        STATUS_MAP.put(ErrorCode.PLAYLIST_CONTENT_NOT_FOUND, HttpStatus.NOT_FOUND);
        STATUS_MAP.put(ErrorCode.PLAYLIST_MINIMUM_CONTENT_REQUIRED, HttpStatus.BAD_REQUEST);
        STATUS_MAP.put(ErrorCode.INVALID_PLAYLIST_CONTENT_REQUEST, HttpStatus.BAD_REQUEST);


        STATUS_MAP.put(ErrorCode.SELF_FOLLOW_NOT_ALLOWED, HttpStatus.BAD_REQUEST);
        STATUS_MAP.put(ErrorCode.FOLLOW_ALREADY_EXISTS, HttpStatus.CONFLICT);
        STATUS_MAP.put(ErrorCode.FOLLOW_NOT_FOUND, HttpStatus.NOT_FOUND);

        // Direct Message
        STATUS_MAP.put(ErrorCode.CONVERSATION_NOT_FOUND, HttpStatus.NOT_FOUND);
        STATUS_MAP.put(ErrorCode.CONVERSATION_ACCESS_DENIED, HttpStatus.FORBIDDEN);
        STATUS_MAP.put(ErrorCode.DIRECT_MESSAGE_NOT_FOUND, HttpStatus.NOT_FOUND);
        STATUS_MAP.put(ErrorCode.SELF_DIRECT_MESSAGE_NOT_ALLOWED, HttpStatus.BAD_REQUEST);

        // TODO: Notification ErrorCode의 HTTP Status 매핑 추가
        // NOTIFICATION_NOT_FOUND -> 404
        // NOTIFICATION_ACCESS_DENIED -> 403

        // Preference
        STATUS_MAP.put(ErrorCode.PREFERENCE_NOT_FOUND, HttpStatus.NOT_FOUND);
        STATUS_MAP.put(ErrorCode.PREFERENCE_ALREADY_EXISTS, HttpStatus.CONFLICT);
        STATUS_MAP.put(ErrorCode.PREFERENCE_CONTENT_NOT_SELECTABLE, HttpStatus.BAD_REQUEST);

        STATUS_MAP.put(
                ErrorCode.INTERNAL_SERVER_ERROR,
                HttpStatus.INTERNAL_SERVER_ERROR
        );

        // WatchParty
        STATUS_MAP.put(ErrorCode.WATCHPARTY_NOT_FOUND, HttpStatus.NOT_FOUND);
        STATUS_MAP.put(ErrorCode.WATCHPARTY_INVALID_EPISODE_RANGE, HttpStatus.BAD_REQUEST);
        STATUS_MAP.put(ErrorCode.WATCHPARTY_ALREADY_ENDED, HttpStatus.CONFLICT);
        STATUS_MAP.put(ErrorCode.WATCHPARTY_HOST_CANNOT_JOIN, HttpStatus.BAD_REQUEST);
        STATUS_MAP.put(ErrorCode.WATCHPARTY_ALREADY_JOINED, HttpStatus.CONFLICT);
        STATUS_MAP.put(ErrorCode.WATCHPARTY_KICKED_CANNOT_REJOIN, HttpStatus.FORBIDDEN);
        STATUS_MAP.put(ErrorCode.WATCHPARTY_CAPACITY_FULL, HttpStatus.CONFLICT);
        STATUS_MAP.put(ErrorCode.WATCHPARTY_PARTICIPANT_NOT_FOUND, HttpStatus.NOT_FOUND);
        STATUS_MAP.put(ErrorCode.WATCHPARTY_NOT_JOINED, HttpStatus.CONFLICT);
        STATUS_MAP.put(ErrorCode.WATCHPARTY_NOT_A_PARTICIPANT, HttpStatus.NOT_FOUND);
        STATUS_MAP.put(ErrorCode.WATCHPARTY_PARTICIPANT_NOT_JOINED, HttpStatus.CONFLICT);
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
