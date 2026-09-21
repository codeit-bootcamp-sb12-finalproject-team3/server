package com.moduplaylist.api.content.service;

import com.github.f4b6a3.uuid.UuidCreator;
import com.moduplaylist.core.activity.enums.ContentActivityType;
import com.moduplaylist.infrastructure.kafka.KafkaTopics;
import com.moduplaylist.infrastructure.kafka.event.ContentViewed;
import com.moduplaylist.infrastructure.redis.content.ContentViewDeduplicator;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentViewActivityService {

	private final ContentViewDeduplicator contentViewDeduplicator;
	private final KafkaTemplate<String, Object> kafkaTemplate;

	public void record(UUID userId, UUID contentId) {
		try {
			if (!contentViewDeduplicator.registerFirstView(userId, contentId)) {
				return;
			}

			ContentViewed event = new ContentViewed(
				UuidCreator.getTimeOrderedEpoch(),
				ContentActivityType.CONTENT_VIEW,
				userId,
				contentId,
				Instant.now()
			);
			kafkaTemplate.send(KafkaTopics.CONTENT_ACTIVITIES, contentId.toString(), event)
				.whenComplete((result, exception) -> {
					if (exception != null) {
						log.warn("ContentViewed 발행에 실패했습니다. contentId={}", contentId, exception);
					}
				});
		} catch (RuntimeException exception) {
			log.warn("콘텐츠 조회 활동 처리에 실패했습니다. contentId={}", contentId, exception);
		}
	}
}
