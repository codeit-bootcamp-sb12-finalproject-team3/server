package com.moduplaylist.core.watchparty.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;

import java.util.UUID;

public class WatchPartyReminderAlreadyExistsException extends BaseException {

    // 서비스에서 exists 확인으로 중복을 발견한 경우
    public WatchPartyReminderAlreadyExistsException(UUID partyId, UUID userId) {
        this(partyId, userId, null);
    }

    // 동시 요청으로 DB UNIQUE 제약 위반이 난 경우
    public WatchPartyReminderAlreadyExistsException(UUID partyId, UUID userId, Throwable cause) {
        super(ErrorCode.WATCHPARTY_REMINDER_ALREADY_EXISTS, cause);
        addDetail("partyId", partyId);
        addDetail("userId", userId);
    }
}