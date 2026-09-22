package com.moduplaylist.core.watchparty.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;

import java.util.UUID;

public class WatchPartyAlreadyJoinedException extends BaseException {
    public WatchPartyAlreadyJoinedException(UUID partyId, UUID userId) {
        super(ErrorCode.WATCHPARTY_ALREADY_JOINED);
        addDetail("partyId", partyId);
        addDetail("userId", userId);
    }
}
