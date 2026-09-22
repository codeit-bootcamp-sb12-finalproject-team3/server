package com.moduplaylist.core.watchparty.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;

import java.util.UUID;

public class WatchPartyInvalidStateException extends BaseException {
    public WatchPartyInvalidStateException(UUID partyId, String message) {
        super(ErrorCode.WATCHPARTY_INVALID_STATE);
        addDetail("partyId", partyId);
        addDetail("reason", message);
    }
}