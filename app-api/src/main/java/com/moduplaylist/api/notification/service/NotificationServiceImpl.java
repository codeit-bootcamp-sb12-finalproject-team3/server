package com.moduplaylist.api.notification.service;

import com.moduplaylist.api.global.dto.CursorPageResponse;
import com.moduplaylist.api.notification.dto.NotificationDto;
import com.moduplaylist.api.notification.dto.NotificationRequest;
import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import com.moduplaylist.core.notification.entity.Notification;
import com.moduplaylist.core.notification.exception.NotificationAccessDeniedException;
import com.moduplaylist.core.notification.exception.NotificationNotFoundException;
import com.moduplaylist.core.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private static final String SORT_BY = "createdAt,id";

    @Override
    public CursorPageResponse<NotificationDto> getNotifications(UUID receiverId, NotificationRequest request) {
        validateReceiver(receiverId);
        //TODO 커서 페이지네이션 들어가요
        return null;
    }

    @Override
    @Transactional
    public void delete(UUID receiverId, UUID notificationId) {
        validateReceiver(receiverId);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));

        if (!notification.getReceiver().getId().equals(receiverId)) {
            throw new NotificationAccessDeniedException(notificationId);
        }

        notificationRepository.delete(notification);
    }


    private void validateReceiver(UUID receiverId) {
        if (receiverId == null) {
            throw new BaseException(ErrorCode.UNAUTHORIZED);
        }
    }
}
