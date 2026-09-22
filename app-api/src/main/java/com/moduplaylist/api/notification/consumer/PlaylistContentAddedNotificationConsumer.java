package com.moduplaylist.api.notification.consumer;

import com.moduplaylist.api.notification.dto.NotificationCreateCommand;
import com.moduplaylist.api.notification.service.NotificationService;
import com.moduplaylist.core.notification.entity.NotificationLevel;
import com.moduplaylist.core.playlist.repository.PlaylistSubscriptionRepository;
import com.moduplaylist.infrastructure.kafka.KafkaTopics;
import com.moduplaylist.infrastructure.kafka.event.PlaylistContentAddedKafkaEvent;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlaylistContentAddedNotificationConsumer {

  private static final int SUBSCRIBER_PAGE_SIZE = 500;

  private final NotificationService notificationService;
  private final PlaylistSubscriptionRepository playlistSubscriptionRepository;

  @KafkaListener(
      topics = KafkaTopics.PLAYLIST_CONTENT_ADDED,
      groupId = "notification-persistence"
  )
  public void consume(PlaylistContentAddedKafkaEvent event) {
    int page = 0;
    Slice<UUID> subscriberSlice;

    do {
      subscriberSlice = playlistSubscriptionRepository.findSubscriberIdsByPlaylistId(
          event.playlistId(),
          PageRequest.of(page, SUBSCRIBER_PAGE_SIZE)
      );

      for (UUID subscriberId : subscriberSlice.getContent()) {
        if (subscriberId.equals(event.ownerId())) {
          continue;
        }

        NotificationCreateCommand command = new NotificationCreateCommand(
            subscriberId,
            "플레이리스트 콘텐츠 추가",
            "구독 중인 '" + event.playlistTitle()
                + "'에 '" + event.contentTitle()
                + "' 콘텐츠가 추가되었습니다.",
            NotificationLevel.INFO
        );

        notificationService.create(command);
      }

      page++;
    } while (subscriberSlice.hasNext());
  }
}
