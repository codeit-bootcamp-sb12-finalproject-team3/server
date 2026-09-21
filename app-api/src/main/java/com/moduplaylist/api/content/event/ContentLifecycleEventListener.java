package com.moduplaylist.api.content.event;

import com.moduplaylist.api.content.service.ContentAutocompleteIndexService;
import com.moduplaylist.infrastructure.kafka.KafkaTopics;
import com.moduplaylist.infrastructure.kafka.event.ContentDeleted;
import com.moduplaylist.infrastructure.kafka.event.ContentUpserted;
import com.moduplaylist.infrastructure.opensearch.content.ContentVectorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentLifecycleEventListener {

	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final ContentAutocompleteIndexService autocompleteIndexService;
	private final ObjectProvider<ContentVectorRepository> contentVectorRepositoryProvider;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(ContentLifecycleEvent event) {
		Object kafkaEvent = switch (event.type()) {
			case UPSERTED -> new ContentUpserted(
				event.eventId(), event.contentId(), event.occurredAt());
			case DELETED -> new ContentDeleted(
				event.eventId(), event.contentId(), event.occurredAt());
		};
		try {
			kafkaTemplate.send(
				KafkaTopics.CONTENT_LIFECYCLE,
				event.contentId().toString(),
				kafkaEvent
			).whenComplete((result, exception) -> {
				if (exception != null) {
					log.warn(
						"콘텐츠 생명주기 이벤트 발행에 실패했습니다. eventId={}, type={}, contentId={}",
						event.eventId(), event.type(), event.contentId(), exception);
				}
			});
		} catch (RuntimeException exception) {
			log.warn(
				"콘텐츠 생명주기 이벤트 발행 요청에 실패했습니다. eventId={}, type={}, contentId={}",
				event.eventId(), event.type(), event.contentId(), exception);
		}

		try {
			autocompleteIndexService.synchronize(event.contentId());
		} catch (RuntimeException exception) {
			log.warn(
				"자동완성 인덱스 동기화에 실패했습니다. eventId={}, type={}, contentId={}",
				event.eventId(), event.type(), event.contentId(), exception);
		}

		if (event.type() == ContentLifecycleEvent.Type.DELETED) {
			deleteVectorDocument(event);
		}
	}

	private void deleteVectorDocument(ContentLifecycleEvent event) {
		ContentVectorRepository contentVectorRepository =
			contentVectorRepositoryProvider.getIfAvailable();
		if (contentVectorRepository == null) {
			return;
		}
		try {
			contentVectorRepository.deleteById(event.contentId());
		} catch (RuntimeException exception) {
			log.warn(
				"숨김 콘텐츠 벡터 문서 삭제에 실패했습니다. eventId={}, contentId={}",
				event.eventId(), event.contentId(), exception);
		}
	}
}
