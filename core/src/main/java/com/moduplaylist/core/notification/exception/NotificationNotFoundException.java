package com.moduplaylist.core.notification.exception;

import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import java.util.UUID;

public class NotificationNotFoundException extends BaseException {

    public NotificationNotFoundException(UUID notificationId) {
        super(ErrorCode.NOTIFICATION_NOT_FOUND);
        addDetail("notificationId", notificationId);
    }
}
