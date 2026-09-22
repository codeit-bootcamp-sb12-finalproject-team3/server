package com.moduplaylist.core.recommendation.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;

import java.util.UUID;

public class PreferenceNotFoundException extends BaseException {

    public PreferenceNotFoundException(UUID userId) {
        super(ErrorCode.PREFERENCE_NOT_FOUND);
        addDetail("userId", userId);
    }
}
