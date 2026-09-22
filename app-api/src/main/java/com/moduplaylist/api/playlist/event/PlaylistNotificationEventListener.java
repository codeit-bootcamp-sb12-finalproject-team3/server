package com.moduplaylist.api.playlist.event;

import com.moduplaylist.infrastructure.kafka.KafkaTopics;
import com.moduplaylist.infrastructure.kafka.event.PlaylistContentAddedKafkaEvent;
import com.moduplaylist.infrastructure.kafka.event.PlaylistSubscribedKafkaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlaylistNotificationEventListener {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(PlaylistSubscribedEvent event) {
    PlaylistSubscribedKafkaEvent kafkaEvent = new PlaylistSubscribedKafkaEvent(
        event.subscriberId(),
        event.ownerId(),
        event.playlistId()
    );

    try {
      kafkaTemplate.send(
          KafkaTopics.PLAYLIST_SUBSCRIBED,
          kafkaEvent
      ).whenComplete((result, exception) -> {
        if (exception != null) {
          log.warn(
              "플레이리스트 구독 알림 이벤트 발행에 실패했습니다. "
                  + "subscriberId={}, ownerId={}, playlistId={}",
              event.subscriberId(),
              event.ownerId(),
              event.playlistId(),
              exception
          );
        }
      });
    } catch (RuntimeException exception) {
      log.warn(
          "플레이리스트 구독 알림 이벤트 발행 요청에 실패했습니다. "
              + "subscriberId={}, ownerId={}, playlistId={}",
          event.subscriberId(),
          event.ownerId(),
          event.playlistId(),
          exception
      );
    }
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(PlaylistContentAddedEvent event) {
    PlaylistContentAddedKafkaEvent kafkaEvent = new PlaylistContentAddedKafkaEvent(
        event.ownerId(),
        event.playlistId(),
        event.contentId(),
        event.playlistTitle(),
        event.contentTitle()
    );

    try {
      kafkaTemplate.send(
          KafkaTopics.PLAYLIST_CONTENT_ADDED,
          kafkaEvent
      ).whenComplete((result, exception) -> {
        if (exception != null) {
          log.warn(
              "플레이리스트 콘텐츠 추가 알림 이벤트 발행에 실패했습니다. "
                  + "playlistId={}, contentId={}",
              event.playlistId(),
              event.contentId(),
              exception
          );
        }
      });
    } catch (RuntimeException exception) {
      log.warn(
          "플레이리스트 콘텐츠 추가 알림 이벤트 발행 요청에 실패했습니다. "
              + "playlistId={}, contentId={}",
          event.playlistId(),
          event.contentId(),
          exception
      );
    }
  }
}
