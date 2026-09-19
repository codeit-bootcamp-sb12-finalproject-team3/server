package com.moduplaylist.api.content.service;

import com.moduplaylist.api.content.dto.ContentCreateRequest;
import com.moduplaylist.api.content.dto.ContentCreateResponse;
import com.moduplaylist.api.content.dto.ContentResponse;
import com.moduplaylist.api.content.dto.ContentUpdateRequest;
import com.moduplaylist.api.content.dto.EpisodeCreateRequest;
import com.moduplaylist.api.content.dto.EpisodeResponse;
import java.util.UUID;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

public interface ContentCommandService {

	ContentCreateResponse create(ContentCreateRequest request, Map<String, MultipartFile> images);

	ContentResponse update(UUID contentId, ContentUpdateRequest request, MultipartFile thumbnail);

	EpisodeResponse createEpisode(
		UUID seasonId,
		EpisodeCreateRequest request,
		MultipartFile thumbnail);

	void delete(UUID contentId);
}
