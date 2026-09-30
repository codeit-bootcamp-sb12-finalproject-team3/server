package com.moduplaylist.core.watchparty.exception;


import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;

import java.util.UUID;

public class WatchPartyHostOnlyException extends BaseException {
    public WatchPartyHostOnlyException(UUID partyId, UUID userId) {
        super(ErrorCode.FORBIDDEN);
        addDetail("partyId", partyId);
        addDetail("userId", userId);
    }
}