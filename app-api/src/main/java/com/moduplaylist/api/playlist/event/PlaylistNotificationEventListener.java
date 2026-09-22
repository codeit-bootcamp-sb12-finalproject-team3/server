package com.moduplaylist.api.playlist.event;

import com.moduplaylist.core.playlist.repository.PlaylistSubscriptionRepository;
import com.moduplaylist.infrastructure.kafka.KafkaTopics;
import com.moduplaylist.infrastructure.kafka.event.PlaylistContentAddedKafkaEvent;
import com.moduplaylist.infrastructure.kafka.event.PlaylistSubscribedKafkaEvent;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PlaylistNotificationEventListener {

  private final PlaylistSubscriptionRepository playlistSubscriptionRepository;
  private final KafkaTemplate<String, Object> kafkaTemplate;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(PlaylistSubscribedEvent event) {
    PlaylistSubscribedKafkaEvent kafkaEvent = new PlaylistSubscribedKafkaEvent(
        event.subscriberId(),
        event.ownerId(),
        event.playlistId()
    );

    kafkaTemplate.send(KafkaTopics.PLAYLIST_SUBSCRIBED, kafkaEvent);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(PlaylistContentAddedEvent event) {
    for (UUID subscriberId
    : playlistSubscriptionRepository.findSubscriberIdsByPlaylistId(event.playlistId())) {

      if (subscriberId.equals(event.ownerId())) {
        continue;
      }

      PlaylistContentAddedKafkaEvent kafkaEvent = new PlaylistContentAddedKafkaEvent(
          subscriberId,
          event.ownerId(),
          event.playlistId(),
          event.contentId(),
          event.playlistTitle(),
          event.contentTitle()
      );

      kafkaTemplate.send(KafkaTopics.PLAYLIST_CONTENT_ADDED, kafkaEvent);
    }
  }
}
