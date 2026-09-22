package com.moduplaylist.core.notification.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import java.util.UUID;

public class NotificationAccessDeniedException extends BaseException {

    public NotificationAccessDeniedException(UUID notificationId) {
        super(ErrorCode.NOTIFICATION_ACCESS_DENIED);
        addDetail("notificationId", notificationId);
    }
}
