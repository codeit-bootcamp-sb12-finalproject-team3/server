package com.moduplaylist.core.watchparty.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;

import java.util.UUID;

public class WatchPartyParticipantNotFoundException extends BaseException {
    public WatchPartyParticipantNotFoundException(UUID partyId, UUID userId) {
        super(ErrorCode.WATCHPARTY_PARTICIPANT_NOT_FOUND);
        addDetail("partyId", partyId);
        addDetail("userId", userId);
    }
}