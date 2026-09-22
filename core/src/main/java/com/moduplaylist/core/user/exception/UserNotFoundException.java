package com.moduplaylist.core.user.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import java.util.UUID;

public class UserNotFoundException extends BaseException {

    public UserNotFoundException(UUID userId) {
        super(ErrorCode.USER_NOT_FOUND);
        addDetail("userId", userId);
    }
}
