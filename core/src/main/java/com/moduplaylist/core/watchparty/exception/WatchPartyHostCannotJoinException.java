package com.moduplaylist.core.watchparty.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;

import java.util.UUID;

public class WatchPartyHostCannotJoinException extends BaseException {
    public WatchPartyHostCannotJoinException(UUID partyId, UUID userId) {
        super(ErrorCode.WATCHPARTY_HOST_CANNOT_JOIN);
        addDetail("partyId", partyId);
        addDetail("userId", userId);
    }
}