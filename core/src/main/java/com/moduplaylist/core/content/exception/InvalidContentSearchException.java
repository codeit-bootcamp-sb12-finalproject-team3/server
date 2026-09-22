package com.moduplaylist.core.content.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;

public class InvalidContentSearchException extends BaseException {

    public InvalidContentSearchException() {
        super(ErrorCode.INVALID_REQUEST);
    }
}
