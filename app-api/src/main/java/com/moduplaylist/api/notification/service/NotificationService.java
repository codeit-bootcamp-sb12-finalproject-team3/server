package com.moduplaylist.api.notification.service;

import com.moduplaylist.api.global.dto.CursorPageResponse;
import com.moduplaylist.api.notification.dto.NotificationDto;
import com.moduplaylist.api.notification.dto.NotificationRequest;

import java.util.UUID;

public interface NotificationService {

    CursorPageResponse<NotificationDto> getNotifications(
            UUID receiverId,
            NotificationRequest request
    );

    void delete(UUID receiverId, UUID notificationId);


}
