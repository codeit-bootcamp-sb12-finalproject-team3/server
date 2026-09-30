package com.moduplaylist.api.review.event;

import com.moduplaylist.infrastructure.kafka.event.ContentActivityKafkaEvent;
import com.moduplaylist.infrastructure.kafka.producer.ContentActivityKafkaPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ReviewRatingChangedEventListener {

	private final ContentActivityKafkaPublisher contentActivityKafkaPublisher;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(ReviewRatingChangedEvent event) {
		ContentActivityKafkaEvent kafkaEvent = new ContentActivityKafkaEvent(
			event.eventId(),
			event.eventType(),
			event.userId(),
			event.contentId(),
			event.oldRating(),
			event.newRating(),
			event.occurredAt()
		);
		contentActivityKafkaPublisher.publish(kafkaEvent);
	}
}
