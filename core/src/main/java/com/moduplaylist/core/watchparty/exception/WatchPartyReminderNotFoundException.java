package com.moduplaylist.core.watchparty.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;

import java.util.UUID;

public class WatchPartyReminderNotFoundException extends BaseException {
    public WatchPartyReminderNotFoundException(UUID partyId, UUID userId) {
        super(ErrorCode.WATCHPARTY_REMINDER_NOT_FOUND);
        addDetail("partyId", partyId);
        addDetail("userId", userId);
    }
}