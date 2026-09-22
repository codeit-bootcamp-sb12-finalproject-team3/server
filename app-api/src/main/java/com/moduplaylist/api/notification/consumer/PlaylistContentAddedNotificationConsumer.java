package com.moduplaylist.api.notification.consumer;

import com.moduplaylist.api.notification.dto.NotificationCreateCommand;
import com.moduplaylist.api.notification.service.NotificationService;
import com.moduplaylist.core.notification.entity.NotificationLevel;
import com.moduplaylist.infrastructure.kafka.KafkaTopics;
import com.moduplaylist.infrastructure.kafka.event.PlaylistContentAddedKafkaEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlaylistContentAddedNotificationConsumer {

  private final NotificationService notificationService;

  @KafkaListener(
      topics = KafkaTopics.PLAYLIST_CONTENT_ADDED,
      groupId = "notification-persistence"
  )
  public void consume(PlaylistContentAddedKafkaEvent event) {
    NotificationCreateCommand command = new NotificationCreateCommand(
        event.receiverId(),
        "플레이리스트 콘텐츠 추가",
        "구독 중인 플레이리스트에 새로운 콘텐츠가 추가되었습니다.",
        NotificationLevel.INFO
    );

    notificationService.create(command);
  }
}
