package com.moduplaylist.core.review.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import java.util.UUID;

public class ReviewNotFoundException extends BaseException {

    public ReviewNotFoundException(UUID reviewId) {
        super(ErrorCode.REVIEW_NOT_FOUND);
        addDetail("reviewId", reviewId);
    }
}
