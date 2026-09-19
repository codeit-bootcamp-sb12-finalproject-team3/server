package com.moduplaylist.api.content.event;

import com.moduplaylist.infrastructure.kafka.KafkaTopics;
import com.moduplaylist.infrastructure.kafka.event.ContentActivityKafkaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentLikeChangedEventListener {

	private final KafkaTemplate<String, Object> kafkaTemplate;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(ContentLikeChangedEvent event) {
		ContentActivityKafkaEvent kafkaEvent = new ContentActivityKafkaEvent(
			event.eventId(),
			event.eventType(),
			event.userId(),
			event.contentId(),
			event.occurredAt()
		);
		String key = event.userId() + ":" + event.contentId();

		try {
			kafkaTemplate.send(KafkaTopics.CONTENT_ACTIVITIES, key, kafkaEvent)
				.whenComplete((result, exception) -> {
					if (exception != null) {
						log.warn(
							"콘텐츠 좋아요 활동 이벤트 발행에 실패했습니다. eventId={}, eventType={}, userId={}, contentId={}",
							event.eventId(),
							event.eventType(),
							event.userId(),
							event.contentId(),
							exception
						);
					}
				});
		} catch (RuntimeException exception) {
			log.warn(
				"콘텐츠 좋아요 활동 이벤트 발행 요청에 실패했습니다. eventId={}, eventType={}, userId={}, contentId={}",
				event.eventId(),
				event.eventType(),
				event.userId(),
				event.contentId(),
				exception
			);
		}
	}
}
