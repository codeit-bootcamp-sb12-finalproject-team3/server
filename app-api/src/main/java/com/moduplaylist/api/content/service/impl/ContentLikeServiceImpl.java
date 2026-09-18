package com.moduplaylist.api.content.service.impl;

import com.moduplaylist.api.content.dto.ContentLikeResponseDto;
import com.moduplaylist.api.content.service.ContentLikeService;
import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.entity.ContentLike;
import com.moduplaylist.core.content.exception.ContentNotFoundException;
import com.moduplaylist.core.content.exception.ContentNotLikeableException;
import com.moduplaylist.core.content.exception.InvalidContentSearchException;
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
		if (!status.getContentType().isLikeable()) {
			throw new ContentNotLikeableException(contentId);
		}
		return response(status.getLiked(), status.getLikeCount());
	}

	@Override
	@Transactional
	public ContentLikeResponseDto like(UUID userId, UUID contentId) {
		Content content = lockContent(contentId);
		if (content.isHidden()) {
			throw new InvalidContentSearchException();
		}
		requireLikeable(content);
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
		Content content = lockContent(contentId);
		requireLikeable(content);
		int deleted = contentLikeRepository.deleteByUserIdAndContentId(userId, contentId);
		if (deleted == 0) {
			return response(false, content.getLikeCount());
		}
		contentLikeRepository.decrementLikeCount(contentId);
		return response(false, Math.max(0, content.getLikeCount() - 1));
	}

	private Content lockContent(UUID contentId) {
		return contentRepository.findByIdForUpdate(contentId)
			.orElseThrow(() -> new ContentNotFoundException(contentId));
	}

	private void requireLikeable(Content content) {
		if (!content.isLikeable()) {
			throw new ContentNotLikeableException(content.getId());
		}
	}

	private ContentLikeResponseDto response(boolean liked, long likeCount) {
		return ContentLikeResponseDto.builder().liked(liked).likeCount(likeCount).build();
	}
}
