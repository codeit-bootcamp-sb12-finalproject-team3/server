package com.moduplaylist.core.watchparty.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;

import java.util.UUID;

public class WatchPartyNotFoundException extends BaseException {
    public WatchPartyNotFoundException(UUID partyId) {
        super(ErrorCode.WATCHPARTY_NOT_FOUND);
        addDetail("partyId", partyId);
    }
}