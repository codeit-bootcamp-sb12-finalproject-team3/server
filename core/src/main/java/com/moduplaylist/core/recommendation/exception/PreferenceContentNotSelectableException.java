package com.moduplaylist.core.recommendation.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import java.util.UUID;

public class PreferenceContentNotSelectableException extends BaseException {

    public PreferenceContentNotSelectableException(UUID contentId) {
        super(ErrorCode.PREFERENCE_CONTENT_NOT_SELECTABLE);
        addDetail("contentId", contentId);
    }
}
