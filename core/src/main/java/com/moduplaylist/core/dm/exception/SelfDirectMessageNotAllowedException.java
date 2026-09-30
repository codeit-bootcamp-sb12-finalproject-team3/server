package com.moduplaylist.core.dm.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import java.util.UUID;

public class SelfDirectMessageNotAllowedException extends BaseException {
    public SelfDirectMessageNotAllowedException(UUID userId) {
        super(ErrorCode.SELF_DIRECT_MESSAGE_NOT_ALLOWED);
        addDetail("userId", userId);
    }
}
