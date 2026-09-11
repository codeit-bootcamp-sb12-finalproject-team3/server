package com.moduplaylist.core.watchparty.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;

import java.util.UUID;

public class WatchPartyInvalidEpisodeRangeException extends BaseException {
    public WatchPartyInvalidEpisodeRangeException(UUID contentId) {
        super(ErrorCode.WATCHPARTY_INVALID_EPISODE_RANGE);
        addDetail("contentId", contentId);
    }
}