package com.moduplaylist.core.content.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import java.util.UUID;

public class ContentSeasonAlreadyExistsException extends BaseException {

    public ContentSeasonAlreadyExistsException(
            UUID parentContentId,
            Integer seasonNumber
    ) {
        this(parentContentId, seasonNumber, null);
    }

    public ContentSeasonAlreadyExistsException(
            UUID parentContentId,
            Integer seasonNumber,
            Throwable cause
    ) {
        super(ErrorCode.CONTENT_SEASON_ALREADY_EXISTS, cause);
        addDetail("parentContentId", parentContentId);
        addDetail("seasonNumber", seasonNumber);
    }
}
