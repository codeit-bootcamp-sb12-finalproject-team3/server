package com.moduplaylist.core.watchparty.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;

import java.util.UUID;

public class WatchPartyMaxParticipantsBelowCurrentException extends BaseException {
    public WatchPartyMaxParticipantsBelowCurrentException(UUID partyId, long currentCount, Integer requestedMaxParticipants) {
        super(ErrorCode.WATCHPARTY_MAX_PARTICIPANTS_BELOW_CURRENT);
        addDetail("partyId", partyId);
        addDetail("currentCount", currentCount);
        addDetail("requestedMaxParticipants", requestedMaxParticipants);
    }
}