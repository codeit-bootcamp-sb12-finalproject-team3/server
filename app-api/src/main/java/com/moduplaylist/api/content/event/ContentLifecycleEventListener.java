package com.moduplaylist.api.content.event;

import com.moduplaylist.infrastructure.kafka.KafkaTopics;
import com.moduplaylist.infrastructure.kafka.event.ContentDeleted;
import com.moduplaylist.infrastructure.kafka.event.ContentUpserted;
import com.moduplaylist.infrastructure.opensearch.content.ContentIndexSource;
import com.moduplaylist.infrastructure.opensearch.content.ContentIndexSynchronizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentLifecycleEventListener {
	private static final int INDEX_SYNC_MAX_ATTEMPTS = 3;

	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final ContentIndexSynchronizer contentIndexSynchronizer;

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

		ContentIndexSource source = loadSource(event);
		if (source == null) {
			return;
		}
		synchronizeAutocomplete(event, source);
		synchronizeSearchDocument(event, source);
	}

	private ContentIndexSource loadSource(ContentLifecycleEvent event) {
		for (int attempt = 1; attempt <= INDEX_SYNC_MAX_ATTEMPTS; attempt++) {
			try {
				return contentIndexSynchronizer.load(event.contentId());
			} catch (RuntimeException exception) {
				if (attempt == INDEX_SYNC_MAX_ATTEMPTS) {
					log.warn(
						"콘텐츠 색인 원본 조회에 최종 실패했습니다. "
							+ "eventId={}, type={}, contentId={}, attempts={}",
						event.eventId(), event.type(), event.contentId(), attempt, exception);
				}
			}
		}
		return null;
	}

	private void synchronizeAutocomplete(
		ContentLifecycleEvent event,
		ContentIndexSource source
	) {
		for (int attempt = 1; attempt <= INDEX_SYNC_MAX_ATTEMPTS; attempt++) {
			try {
				contentIndexSynchronizer.synchronizeAutocomplete(source);
				return;
			} catch (RuntimeException exception) {
				if (attempt == INDEX_SYNC_MAX_ATTEMPTS) {
					log.warn(
						"자동완성 인덱스 동기화에 최종 실패했습니다. eventId={}, type={}, contentId={}, attempts={}",
						event.eventId(), event.type(), event.contentId(), attempt, exception);
				}
			}
		}
	}

	private void synchronizeSearchDocument(
		ContentLifecycleEvent event,
		ContentIndexSource source
	) {
		for (int attempt = 1; attempt <= INDEX_SYNC_MAX_ATTEMPTS; attempt++) {
			try {
				contentIndexSynchronizer.synchronizeSearch(source);
				return;
			} catch (RuntimeException exception) {
				if (attempt == INDEX_SYNC_MAX_ATTEMPTS) {
					log.warn(
						"콘텐츠 검색 문서 동기화에 최종 실패했습니다. "
							+ "eventId={}, type={}, contentId={}, attempts={}",
						event.eventId(), event.type(), event.contentId(), attempt, exception);
				}
			}
		}
	}
}
