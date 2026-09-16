package com.moduplaylist.core.dm.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import java.util.UUID;

public class DirectMessageNotFoundException extends BaseException {
    public DirectMessageNotFoundException(UUID messageId) {
        super(ErrorCode.DIRECT_MESSAGE_NOT_FOUND);
        addDetail("messageId", messageId);
    }
}
