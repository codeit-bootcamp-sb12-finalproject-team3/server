package com.moduplaylist.core.watchparty.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;

import java.time.Instant;
import java.util.UUID;

public class WatchPartyLobbyNotOpenException extends BaseException {
    public WatchPartyLobbyNotOpenException(UUID partyId, Instant opensAt) {
        super(ErrorCode.WATCHPARTY_LOBBY_NOT_OPEN);
        addDetail("partyId", partyId);
        addDetail("opensAt", opensAt); // 프론트가 "20:30부터 입장 가능"을 보여줄 수 있게
    }
}