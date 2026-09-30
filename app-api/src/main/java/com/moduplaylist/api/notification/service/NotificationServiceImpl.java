package com.moduplaylist.api.notification.service;

import com.moduplaylist.api.global.dto.CursorPageResponse;
import com.moduplaylist.api.global.dto.SortDirection;
import com.moduplaylist.api.notification.dto.NotificationCreateCommand;
import com.moduplaylist.api.notification.dto.NotificationDto;
import com.moduplaylist.api.notification.dto.NotificationRequest;
import com.moduplaylist.api.notification.event.NotificationCreatedEvent;
import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import com.moduplaylist.core.notification.entity.Notification;
import com.moduplaylist.core.notification.exception.NotificationAccessDeniedException;
import com.moduplaylist.core.notification.exception.NotificationNotFoundException;
import com.moduplaylist.core.notification.repository.NotificationRepository;
import com.moduplaylist.core.user.entity.User;
import com.moduplaylist.core.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private static final String SORT_BY = "createdAt,id";

    @Override
    @Transactional
    public void create(NotificationCreateCommand command) {
        validateCreateCommand(command);

        User receiver = userRepository.findById(command.getReceiverId())
                .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));

        Notification notification = Notification.create(
                receiver,
                command.getTitle(),
                command.getContent(),
                command.getLevel()
        );

        Notification savedNotification =
                notificationRepository.save(notification);

        eventPublisher.publishEvent(
                new NotificationCreatedEvent(
                        savedNotification.getId(),
                        savedNotification.getReceiver().getId(),
                        savedNotification.getTitle(),
                        savedNotification.getContent(),
                        savedNotification.getLevel(),
                        savedNotification.getCreatedAt()
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<NotificationDto> getNotifications(
            UUID receiverId,
            NotificationRequest request
    ) {

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


    private void validateCreateCommand(NotificationCreateCommand command) {
        if (command == null) {
            throw invalidCreateRequest("command", "알림 생성 요청이 필요합니다.");
        }
        if (command.getReceiverId() == null) {
            throw invalidCreateRequest("receiverId", "receiverId가 필요합니다.");
        }
        if (command.getTitle() == null
                || command.getTitle().isBlank()
                || command.getTitle().length() > 100) {
            throw invalidCreateRequest("title", "title은 1자 이상 100자 이하여야 합니다.");
        }
        if (command.getContent() == null
                || command.getContent().isBlank()
                || command.getContent().length() > 500) {
            throw invalidCreateRequest("content", "content는 1자 이상 500자 이하여야 합니다.");
        }
        if (command.getLevel() == null) {
            throw invalidCreateRequest("level", "level이 필요합니다.");
        }
    }

    private BaseException invalidCreateRequest(String key, String message) {
        BaseException exception = new BaseException(ErrorCode.INVALID_REQUEST);
        exception.addDetail(key, message);
        return exception;
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
