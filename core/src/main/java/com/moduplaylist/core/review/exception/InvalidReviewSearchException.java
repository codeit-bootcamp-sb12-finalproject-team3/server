package com.moduplaylist.core.review.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;

public class InvalidReviewSearchException extends BaseException {

    public InvalidReviewSearchException() {
        super(ErrorCode.INVALID_REQUEST);
    }
}
