package com.moduplaylist.core.recommendation.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import java.util.UUID;

public class PreferenceAlreadyExistsException extends BaseException {

    public PreferenceAlreadyExistsException(UUID userId) {
        super(ErrorCode.PREFERENCE_ALREADY_EXISTS);
        addDetail("userId", userId);
    }
}
