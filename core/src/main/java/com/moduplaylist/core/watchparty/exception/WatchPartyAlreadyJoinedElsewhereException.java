package com.moduplaylist.core.watchparty.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;

import java.util.UUID;

public class WatchPartyAlreadyJoinedElsewhereException extends BaseException {
    public WatchPartyAlreadyJoinedElsewhereException(UUID userId, UUID partyId) {
        super(ErrorCode.WATCHPARTY_ALREADY_JOINED_ELSEWHERE);
        addDetail("userId", userId);
        addDetail("partyId", partyId);
    }
}