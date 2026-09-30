package com.moduplaylist.core.dm.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;

public class InvalidDirectMessageContentException extends BaseException {

    public InvalidDirectMessageContentException() {
        super(ErrorCode.DIRECT_MESSAGE_CONTENT_INVALID);
        addDetail("content", "content는 1자 이상 255자 이하여야 합니다.");
    }
}
