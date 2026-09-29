package com.moduplaylist.core.watchparty.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;

import java.util.UUID;

public class WatchPartyHostCannotSetReminderException extends BaseException {
    public WatchPartyHostCannotSetReminderException(UUID partyId, UUID userId) {
        super(ErrorCode.WATCHPARTY_HOST_CANNOT_SET_REMINDER);
        addDetail("partyId", partyId);
        addDetail("userId", userId);
    }
}