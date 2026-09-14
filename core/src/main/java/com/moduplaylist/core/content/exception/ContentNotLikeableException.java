package com.moduplaylist.core.content.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import java.util.UUID;

public class ContentNotLikeableException extends BaseException {

    public ContentNotLikeableException(UUID contentId) {
        super(ErrorCode.CONTENT_NOT_LIKEABLE);
        addDetail("contentId", contentId);
    }
}
