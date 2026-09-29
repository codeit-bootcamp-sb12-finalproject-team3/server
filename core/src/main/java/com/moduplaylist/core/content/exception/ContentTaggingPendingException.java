package com.moduplaylist.core.content.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import java.util.UUID;

public class ContentTaggingPendingException extends BaseException {
    public ContentTaggingPendingException(UUID contentId) {
        super(ErrorCode.CONTENT_TAGGING_PENDING);
        addDetail("contentId", contentId);
    }
}
