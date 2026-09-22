package com.moduplaylist.core.review.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import java.util.UUID;

public class ContentNotReviewableException extends BaseException {

    public ContentNotReviewableException(UUID contentId) {
        super(ErrorCode.CONTENT_NOT_REVIEWABLE);
        addDetail("contentId", contentId);
    }
}
