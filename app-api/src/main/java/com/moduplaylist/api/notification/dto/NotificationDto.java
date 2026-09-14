package com.moduplaylist.api.notification.dto;

import com.moduplaylist.core.notification.entity.Notification;
import com.moduplaylist.core.notification.entity.NotificationLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
    private UUID id;
    private Instant createdAt;
    private UUID receiverId;
    private String title;
    private String content;
    private String level;

    public static NotificationDto from(Notification notification) {
        return new NotificationDto(
              notification.getId(),
              notification.getCreatedAt(),
              notification.getReceiver().getId(),
              notification.getTitle(),
              notification.getContent(),
              notification.getLevel().toString()
        );
    }
}

