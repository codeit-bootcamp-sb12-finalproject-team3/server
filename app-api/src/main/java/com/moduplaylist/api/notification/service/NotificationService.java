package com.moduplaylist.api.notification.service;

import com.moduplaylist.api.global.dto.CursorPageResponse;
import com.moduplaylist.api.notification.dto.NotificationCreateCommand;
import com.moduplaylist.api.notification.dto.NotificationDto;
import com.moduplaylist.api.notification.dto.NotificationRequest;

import java.util.UUID;

public interface NotificationService {

    void create(NotificationCreateCommand command);

    CursorPageResponse<NotificationDto> getNotifications(
            UUID receiverId,
            NotificationRequest request
    );

    void delete(UUID receiverId, UUID notificationId);


}
