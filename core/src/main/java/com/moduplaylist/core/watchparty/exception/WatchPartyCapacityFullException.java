package com.moduplaylist.core.watchparty.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;

import java.util.UUID;

public class WatchPartyCapacityFullException extends BaseException {
    public WatchPartyCapacityFullException(UUID partyId) {
        super(ErrorCode.WATCHPARTY_CAPACITY_FULL);
        addDetail("partyId", partyId);
    }
}