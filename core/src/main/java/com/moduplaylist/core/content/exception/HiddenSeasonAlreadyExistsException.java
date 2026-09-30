package com.moduplaylist.core.content.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import java.util.UUID;

public class HiddenSeasonAlreadyExistsException extends BaseException {

    public HiddenSeasonAlreadyExistsException(
            UUID seriesId,
            Integer seasonNumber,
            UUID hiddenSeasonId
    ) {
        super(ErrorCode.HIDDEN_SEASON_ALREADY_EXISTS);
        addDetail("seriesId", seriesId);
        addDetail("seasonNumber", seasonNumber);
        addDetail("hiddenSeasonId", hiddenSeasonId);
    }
}
