package com.moduplaylist.core.content.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;

public class ContentSearchUnavailableException extends BaseException {

    public ContentSearchUnavailableException() {
        super(ErrorCode.CONTENT_SEARCH_UNAVAILABLE);
    }

    public ContentSearchUnavailableException(Throwable cause) {
        super(ErrorCode.CONTENT_SEARCH_UNAVAILABLE, cause);
    }
}
