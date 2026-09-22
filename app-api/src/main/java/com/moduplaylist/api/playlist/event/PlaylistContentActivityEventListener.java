package com.moduplaylist.api.playlist.event;

import com.moduplaylist.infrastructure.kafka.event.ContentActivityKafkaEvent;
import com.moduplaylist.infrastructure.kafka.producer.ContentActivityKafkaPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PlaylistContentActivityEventListener {

  private final ContentActivityKafkaPublisher contentActivityKafkaPublisher;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(PlaylistContentActivityEvent event) {
    ContentActivityKafkaEvent kafkaEvent = new ContentActivityKafkaEvent(
        event.eventId(),
        event.eventType(),
        event.userId(),
        event.contentId(),
        event.occurredAt()
    );

    contentActivityKafkaPublisher.publish(kafkaEvent);
  }
}
