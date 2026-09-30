package com.moduplaylist.core.watchparty.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;

import java.util.UUID;

public class WatchPartyKickedCannotRejoinException extends BaseException {
    public WatchPartyKickedCannotRejoinException(UUID partyId, UUID userId) {
        super(ErrorCode.WATCHPARTY_KICKED_CANNOT_REJOIN);
        addDetail("partyId", partyId);
        addDetail("userId", userId);
    }
}