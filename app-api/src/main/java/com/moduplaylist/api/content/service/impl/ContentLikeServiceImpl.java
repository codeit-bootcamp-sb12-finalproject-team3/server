package com.moduplaylist.api.content.service.impl;

import com.moduplaylist.api.content.dto.ContentLikeResponseDto;
import com.moduplaylist.api.content.service.ContentLikeService;
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
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContentLikeServiceImpl implements ContentLikeService {

	private final ContentRepository contentRepository;
	private final ContentLikeRepository contentLikeRepository;
	private final UserRepository userRepository;

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
		contentLikeRepository.save(ContentLike.create(user, content));
		contentLikeRepository.incrementLikeCount(contentId);
		return response(true, content.getLikeCount() + 1);
	}

	@Override
	@Transactional
	public ContentLikeResponseDto unlike(UUID userId, UUID contentId) {
		Content content = lockViewableContent(contentId);
		int deleted = contentLikeRepository.deleteByUserIdAndContentId(userId, contentId);
		if (deleted == 0) {
			return response(false, content.getLikeCount());
		}
		contentLikeRepository.decrementLikeCount(contentId);
		return response(false, Math.max(0, content.getLikeCount() - 1));
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
