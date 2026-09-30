package com.moduplaylist.api.notification.consumer;

import com.moduplaylist.api.notification.dto.NotificationCreateCommand;
import com.moduplaylist.api.notification.service.NotificationService;
import com.moduplaylist.core.notification.entity.NotificationLevel;
import com.moduplaylist.infrastructure.kafka.KafkaTopics;
import com.moduplaylist.infrastructure.kafka.event.PlaylistSubscribedKafkaEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlaylistSubscribedNotificationConsumer {

  private final NotificationService notificationService;

  @KafkaListener(
      topics = KafkaTopics.PLAYLIST_SUBSCRIBED,
      groupId = "notification-persistence"
  )
  public void consume(PlaylistSubscribedKafkaEvent event) {
    NotificationCreateCommand command = new NotificationCreateCommand(
        event.ownerId(),
        "플레이리스트 구독",
        "회원님의 플레이리스트에 새로운 구독자가 생겼습니다.",
        NotificationLevel.INFO
    );
    notificationService.create(command);
  }
}
