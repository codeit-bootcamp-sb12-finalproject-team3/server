package com.moduplaylist.api.content.service;

import com.moduplaylist.api.content.dto.ContentCreateRequest;
import com.moduplaylist.api.content.dto.ContentCreateResponse;
import com.moduplaylist.api.content.dto.ContentResponse;
import com.moduplaylist.api.content.dto.ContentUpdateRequest;
import com.moduplaylist.api.content.dto.EpisodeCreateRequest;
import com.moduplaylist.api.content.dto.EpisodeResponse;
import com.moduplaylist.api.content.dto.EpisodeUpdateRequest;
import java.util.UUID;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

public interface ContentCommandService {

	ContentCreateResponse create(ContentCreateRequest request, Map<String, MultipartFile> images);

	ContentResponse update(UUID contentId, ContentUpdateRequest request, MultipartFile thumbnail);

	ContentResponse restoreSeason(UUID hiddenSeasonId);

	EpisodeResponse createEpisode(
		UUID seasonId,
		EpisodeCreateRequest request,
		MultipartFile thumbnail);

	EpisodeResponse updateEpisode(
		UUID seasonId,
		UUID episodeId,
		EpisodeUpdateRequest request,
		MultipartFile thumbnail);

	void deleteEpisode(UUID seasonId, UUID episodeId);

	void delete(UUID contentId);
}
