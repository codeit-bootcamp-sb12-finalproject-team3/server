package com.moduplaylist.api.content.event;

import com.moduplaylist.infrastructure.kafka.event.ContentActivityKafkaEvent;
import com.moduplaylist.infrastructure.kafka.producer.ContentActivityKafkaPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ContentLikeChangedEventListener {

	private final ContentActivityKafkaPublisher contentActivityKafkaPublisher;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(ContentLikeChangedEvent event) {
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
