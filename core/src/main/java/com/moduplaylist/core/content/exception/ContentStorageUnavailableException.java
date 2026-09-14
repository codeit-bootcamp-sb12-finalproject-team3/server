package com.moduplaylist.core.content.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;

public class ContentStorageUnavailableException extends BaseException {

    public ContentStorageUnavailableException(Throwable cause) {
        super(ErrorCode.CONTENT_STORAGE_UNAVAILABLE, cause);
    }
}
