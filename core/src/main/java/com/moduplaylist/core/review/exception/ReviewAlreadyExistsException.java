package com.moduplaylist.core.review.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import java.util.UUID;

public class ReviewAlreadyExistsException extends BaseException {

    public ReviewAlreadyExistsException(UUID userId, UUID contentId) {
        this(userId, contentId, null);
    }

    public ReviewAlreadyExistsException(
            UUID userId,
            UUID contentId,
            Throwable cause
    ) {
        super(ErrorCode.REVIEW_ALREADY_EXISTS, cause);
        addDetail("userId", userId);
        addDetail("contentId", contentId);
    }
}
