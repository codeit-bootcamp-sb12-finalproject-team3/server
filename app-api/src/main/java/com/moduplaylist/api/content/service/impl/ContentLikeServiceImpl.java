package com.moduplaylist.api.content.service.impl;

import com.github.f4b6a3.uuid.UuidCreator;
import com.moduplaylist.api.content.dto.ContentLikeResponseDto;
import com.moduplaylist.api.content.event.ContentLikeChangedEvent;
import com.moduplaylist.api.content.service.ContentLikeService;
import com.moduplaylist.core.activity.enums.ContentActivityType;
import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.entity.ContentLike;
import com.moduplaylist.core.content.entity.ContentType;
import com.moduplaylist.core.content.exception.ContentNotFoundException;
import com.moduplaylist.core.content.exception.ContentTypeNotViewableException;
import com.moduplaylist.core.content.repository.ContentLikeRepository;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.core.user.entity.User;
import com.moduplaylist.core.user.exception.UserNotFoundException;
import com.moduplaylist.core.user.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContentLikeServiceImpl implements ContentLikeService {

	private final ContentRepository contentRepository;
	private final ContentLikeRepository contentLikeRepository;
	private final UserRepository userRepository;
	private final ApplicationEventPublisher eventPublisher;

	@Override
	@Transactional(readOnly = true)
	public ContentLikeResponseDto get(UUID userId, UUID contentId) {
		ContentLikeRepository.LikeStatus status = contentLikeRepository.findStatus(userId, contentId)
			.orElseThrow(() -> new ContentNotFoundException(contentId));
		if (status.getContentType() == ContentType.TV_SERIES) {
			throw new ContentTypeNotViewableException(contentId, status.getContentType());
		}
		return response(status.getLiked(), status.getLikeCount());
	}

	@Override
	@Transactional
	public ContentLikeResponseDto like(UUID userId, UUID contentId) {
		Content content = lockViewableContent(contentId);
		if (contentLikeRepository.existsByUser_IdAndContent_Id(userId, contentId)) {
			return response(true, content.getLikeCount());
		}
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new UserNotFoundException(userId));
		ContentLike contentLike = contentLikeRepository.saveAndFlush(ContentLike.create(user, content));
		int updated = contentLikeRepository.incrementLikeCount(contentId);
		if (updated != 1) {
			throw new IllegalStateException("콘텐츠 좋아요 수 증가에 실패했습니다.");
		}
		long likeCount = contentLikeRepository.findLikeCountByContentId(contentId)
			.orElseThrow(() -> new IllegalStateException("콘텐츠 좋아요 수를 조회할 수 없습니다."));
		eventPublisher.publishEvent(new ContentLikeChangedEvent(
			contentLike.getId(),
			ContentActivityType.CONTENT_LIKE,
			userId,
			contentId,
			contentLike.getCreatedAt()
		));
		return response(true, likeCount);
	}

	@Override
	@Transactional
	public ContentLikeResponseDto unlike(UUID userId, UUID contentId) {
		lockViewableContent(contentId);
		int deleted = contentLikeRepository.deleteByUserIdAndContentId(userId, contentId);
		if (deleted != 0) {
			if (deleted != 1) {
				throw new IllegalStateException("콘텐츠 좋아요 관계 삭제 결과가 올바르지 않습니다.");
			}
			int updated = contentLikeRepository.decrementLikeCount(contentId);
			if (updated != 1) {
				throw new IllegalStateException("콘텐츠 좋아요 수 감소에 실패했습니다.");
			}
			eventPublisher.publishEvent(new ContentLikeChangedEvent(
				UuidCreator.getTimeOrderedEpoch(),
				ContentActivityType.CONTENT_UNLIKE,
				userId,
				contentId,
				Instant.now()
			));
		}
		long likeCount = contentLikeRepository.findLikeCountByContentId(contentId)
			.orElseThrow(() -> new IllegalStateException("콘텐츠 좋아요 수를 조회할 수 없습니다."));
		return response(false, likeCount);
	}

	private Content lockViewableContent(UUID contentId) {
		Content content = contentRepository.findByIdForUpdate(contentId)
			.orElseThrow(() -> new ContentNotFoundException(contentId));
		if (content.isHidden()) {
			throw new ContentNotFoundException(contentId);
		}
		if (content.getType() == ContentType.TV_SERIES) {
			throw new ContentTypeNotViewableException(contentId, content.getType());
		}
		return content;
	}

	private ContentLikeResponseDto response(boolean liked, long likeCount) {
		return ContentLikeResponseDto.builder().liked(liked).likeCount(likeCount).build();
	}
}
