package com.moduplaylist.api.notification.consumer;

import com.moduplaylist.api.notification.dto.NotificationCreateCommand;
import com.moduplaylist.api.notification.service.NotificationService;
import com.moduplaylist.core.dm.repository.DmActiveConversationRegistry;
import com.moduplaylist.core.notification.entity.NotificationLevel;
import com.moduplaylist.infrastructure.kafka.KafkaTopics;
import com.moduplaylist.infrastructure.kafka.event.DmMessageCreatedKafkaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DmMessageCreatedNotificationConsumer {

    private final NotificationService notificationService;
    private final DmActiveConversationRegistry activeConversationRegistry;

    @KafkaListener(
            topics = KafkaTopics.DM_MESSAGE_CREATED,
            groupId = "notification-persistence"
    )
    public void consume(DmMessageCreatedKafkaEvent event) {
        try {
            if (activeConversationRegistry.isActive(
                    event.receiverId(),
                    event.conversationId()
            )) {
                return;
            }
        } catch (DataAccessException exception) {
            log.warn(
                    "Failed to read active DM conversation; creating notification. receiverId={}, conversationId={}",
                    event.receiverId(),
                    event.conversationId(),
                    exception
            );
        }

        NotificationCreateCommand command = new NotificationCreateCommand(
                event.receiverId(),
                "새 메시지",
                "새 메시지가 도착했습니다.",
                NotificationLevel.INFO
        );

        notificationService.create(command);
    }
}
