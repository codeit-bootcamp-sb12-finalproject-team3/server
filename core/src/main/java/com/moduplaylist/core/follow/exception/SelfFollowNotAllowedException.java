package com.moduplaylist.core.follow.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;

import java.util.UUID;

public class SelfFollowNotAllowedException extends BaseException {

    public SelfFollowNotAllowedException(UUID userId) {
        super(ErrorCode.SELF_FOLLOW_NOT_ALLOWED);
        addDetail("userId", userId);
    }
}