package com.moduplaylist.core.watchparty.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;

import java.util.UUID;

public class WatchPartyAlreadyEndedException extends BaseException {
    public WatchPartyAlreadyEndedException(UUID partyId) {
        super(ErrorCode.WATCHPARTY_ALREADY_ENDED);
        addDetail("partyId", partyId);
    }
}