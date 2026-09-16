package com.moduplaylist.api.notification.dto;

import com.moduplaylist.core.notification.entity.NotificationLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationCreateCommand {
    private UUID receiverId;
    private String title;
    private String content;
    private NotificationLevel level;
}
