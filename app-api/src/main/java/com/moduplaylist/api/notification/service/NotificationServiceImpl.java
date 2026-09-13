package com.moduplaylist.api.notification.service;

import com.moduplaylist.api.global.dto.CursorPageResponse;
import com.moduplaylist.api.global.dto.SortDirection;
import com.moduplaylist.api.notification.dto.NotificationDto;
import com.moduplaylist.api.notification.dto.NotificationRequest;
import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import com.moduplaylist.core.notification.entity.Notification;
import com.moduplaylist.core.notification.exception.NotificationAccessDeniedException;
import com.moduplaylist.core.notification.exception.NotificationNotFoundException;
import com.moduplaylist.core.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private static final String SORT_BY = "createdAt,id";

    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<NotificationDto> getNotifications(
            UUID receiverId,
            NotificationRequest request
    ) {
        System.out.println("receiverId = " + receiverId);
        System.out.println("cursor = " + request.getCursor());
        System.out.println("idAfter = " + request.getIdAfter());
        System.out.println("limit = " + request.getLimit());
        System.out.println("sortDirection = " + request.getSortDirection());

        validateReceiver(receiverId);
        validateRequest(request);

        Instant cursor = parseCursor(request.getCursor());
        UUID idAfter = parseIdAfter(request.getIdAfter());

        Pageable pageable = PageRequest.of(0, request.getLimit());

        Slice<Notification> slice =
                findNotifications(receiverId, cursor, idAfter, pageable);

        return createCursorPageResponse(receiverId, slice);
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

    private void validateRequest(NotificationRequest request) {
        String cursor = request.getCursor();
        String idAfter = request.getIdAfter();
        int limit = request.getLimit();

        boolean cursorEmpty = cursor == null || cursor.isBlank();
        boolean idAfterEmpty = idAfter == null || idAfter.isBlank();

        if (cursorEmpty != idAfterEmpty) {throw new BaseException(ErrorCode.INVALID_REQUEST);}

        if (limit < 1 || limit > 100) {throw new BaseException(ErrorCode.INVALID_REQUEST);}
    }

    private Instant parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) { return null; }
        try {
            return Instant.parse(cursor);
        } catch (DateTimeParseException e) {
            throw new BaseException(ErrorCode.INVALID_REQUEST);
        }
    }

    private UUID parseIdAfter(String idAfter) {
        if (idAfter == null || idAfter.isBlank()) { return null; }
        try {
            return UUID.fromString(idAfter);
        } catch (IllegalArgumentException e) {
            throw new BaseException(ErrorCode.INVALID_REQUEST);
        }
    }

    private Slice<Notification> findNotifications(
            UUID receiverId,
            Instant cursor,
            UUID idAfter,
            Pageable pageable
    ) {
        if (cursor == null) {
            return notificationRepository
                    .findByReceiverIdOrderByCreatedAtDescIdDesc(
                            receiverId,
                            pageable
                    );
        }
        return notificationRepository.findNextPage(receiverId, cursor, idAfter, pageable);
    }

    private CursorPageResponse<NotificationDto> createCursorPageResponse(
            UUID receiverId,
            Slice<Notification> slice
    ) {
        List<Notification> notifications = slice.getContent();

        List<NotificationDto> notificationDtos = notifications.stream()
                .map(NotificationDto::from)
                .toList();

        Notification lastNotification = getLastNotification(slice, notifications);

        String nextCursor = null;
        UUID nextIdAfter = null;

        if (lastNotification != null) {
            nextCursor = lastNotification.getCreatedAt().toString();
            nextIdAfter = lastNotification.getId();
        }

        return CursorPageResponse.<NotificationDto>builder()
                .data(notificationDtos)
                .nextCursor(nextCursor)
                .nextIdAfter(nextIdAfter)
                .hasNext(slice.hasNext())
                .totalCount(notificationRepository.countByReceiverId(receiverId))
                .sortBy(SORT_BY)
                .sortDirection(SortDirection.DESCENDING)
                .build();
    }

    private Notification getLastNotification(
            Slice<Notification> slice,
            List<Notification> notifications
    ) {
        if (!slice.hasNext() || notifications.isEmpty()) {return null;}
        return notifications.get(notifications.size() - 1);
    }
}
