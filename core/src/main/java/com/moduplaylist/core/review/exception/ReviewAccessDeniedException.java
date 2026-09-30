package com.moduplaylist.core.review.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import java.util.UUID;

public class ReviewAccessDeniedException extends BaseException {

    public ReviewAccessDeniedException(UUID reviewId, UUID userId) {
        super(ErrorCode.REVIEW_ACCESS_DENIED);
        addDetail("reviewId", reviewId);
        addDetail("userId", userId);
    }
}
