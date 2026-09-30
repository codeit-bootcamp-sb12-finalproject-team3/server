package com.moduplaylist.core.watchparty.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;

import java.util.UUID;

public class WatchPartyChatAccessDeniedException extends BaseException {
    public WatchPartyChatAccessDeniedException(UUID partyId, UUID userId) {
        super(ErrorCode.WATCHPARTY_CHAT_ACCESS_DENIED);
        addDetail("partyId", partyId);
        addDetail("userId", userId);
    }
}