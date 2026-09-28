package com.moduplaylist.core.watchparty.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;

public class WatchPartyMaxParticipantsExceededException extends BaseException {
    public WatchPartyMaxParticipantsExceededException(Integer requestedMaxParticipants, int limit) {
        super(ErrorCode.WATCHPARTY_MAX_PARTICIPANTS_EXCEEDED);
        addDetail("requestedMaxParticipants", requestedMaxParticipants);
        addDetail("limit", limit);
    }
}