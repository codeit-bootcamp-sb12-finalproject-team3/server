package com.moduplaylist.core.content.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import java.util.UUID;

public class ContentNotFoundException extends BaseException {

    public ContentNotFoundException(UUID contentId) {
        super(ErrorCode.CONTENT_NOT_FOUND);
        addDetail("contentId", contentId);
    }
}
