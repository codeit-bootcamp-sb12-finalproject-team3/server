package com.moduplaylist.api.content.service;

import com.moduplaylist.api.content.dto.ContentLikeResponseDto;
import java.util.UUID;

public interface ContentLikeService {

	ContentLikeResponseDto get(UUID userId, UUID contentId);

	ContentLikeResponseDto like(UUID userId, UUID contentId);

	ContentLikeResponseDto unlike(UUID userId, UUID contentId);
}
