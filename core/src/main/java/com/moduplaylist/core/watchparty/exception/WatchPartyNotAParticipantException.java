package com.moduplaylist.core.watchparty.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;

import java.util.UUID;

public class WatchPartyNotAParticipantException extends BaseException {
    public WatchPartyNotAParticipantException(UUID partyId, UUID userId) {
        super(ErrorCode.WATCHPARTY_NOT_A_PARTICIPANT);
        addDetail("partyId", partyId);
        addDetail("userId", userId);
    }
}
