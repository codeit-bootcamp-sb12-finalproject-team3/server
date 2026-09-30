package com.moduplaylist.core.content.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import java.util.UUID;

public class ContentDeletionBlockedException extends BaseException {

    public ContentDeletionBlockedException(UUID contentId) {
        this(contentId, null);
    }

    public ContentDeletionBlockedException(UUID contentId, Throwable cause) {
        super(ErrorCode.CONTENT_DELETION_BLOCKED, cause);
        addDetail("contentId", contentId);
    }
}
