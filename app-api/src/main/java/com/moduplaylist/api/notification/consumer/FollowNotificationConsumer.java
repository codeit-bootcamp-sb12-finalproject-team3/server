package com.moduplaylist.api.notification.consumer;

import com.moduplaylist.api.notification.dto.NotificationCreateCommand;
import com.moduplaylist.api.notification.service.NotificationService;
import com.moduplaylist.core.notification.entity.NotificationLevel;
import com.moduplaylist.infrastructure.kafka.KafkaTopics;
import com.moduplaylist.infrastructure.kafka.event.FollowCreatedKafkaEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FollowNotificationConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = KafkaTopics.FOLLOW_CREATED,
            groupId = "notification-persistence"
    )
    public void consume(FollowCreatedKafkaEvent event) {

        NotificationCreateCommand command =
                new NotificationCreateCommand(
                        event.followeeId(),
                        "새로운 팔로워",
                        "새로운 사용자가 회원님을 팔로우했습니다.",
                        NotificationLevel.INFO
                );

        notificationService.create(command);
    }
}