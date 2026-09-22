package com.moduplaylist.api.content.controller;

import com.moduplaylist.api.content.dto.ContentCreateRequest;
import com.moduplaylist.api.content.dto.ContentCreateResponse;
import com.moduplaylist.api.content.dto.ContentResponse;
import com.moduplaylist.api.content.dto.ContentUpdateRequest;
import com.moduplaylist.api.content.dto.EpisodeCreateRequest;
import com.moduplaylist.api.content.dto.EpisodeResponse;
import com.moduplaylist.api.content.dto.EpisodeUpdateRequest;
import com.moduplaylist.api.content.service.ContentCommandService;
import com.moduplaylist.core.content.exception.InvalidContentSearchException;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/contents")
@PreAuthorize("hasRole('ADMIN')")
public class ContentCommandController {

	private final ContentCommandService contentCommandService;

	@PostMapping(consumes = "multipart/form-data")
	public ResponseEntity<ContentCreateResponse> create(
		@Valid @RequestPart("request") ContentCreateRequest request,
		@RequestParam(required = false) MultiValueMap<String, MultipartFile> files
	) {
		if (files != null && files.values().stream().anyMatch(values -> values.size() != 1)) {
			throw new InvalidContentSearchException();
		}
		Map<String, MultipartFile> images = files == null
			? Map.of()
			: files.toSingleValueMap();
		ContentCreateResponse response = contentCommandService.create(request, images);
		UUID locationId = response.getSeriesId() != null
			? response.getSeriesId()
			: response.getContentIds().get(0);
		return ResponseEntity.created(URI.create("/api/contents/" + locationId)).body(response);
	}

	@PatchMapping(value = "/{contentId}", consumes = "multipart/form-data")
	public ResponseEntity<ContentResponse> update(
		@PathVariable UUID contentId,
		@Valid @RequestPart("request") ContentUpdateRequest request,
		@RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail
	) {
		return ResponseEntity.ok(contentCommandService.update(contentId, request, thumbnail));
	}

	@PostMapping(value = "/{seasonId}/episodes", consumes = "multipart/form-data")
	public ResponseEntity<EpisodeResponse> createEpisode(
		@PathVariable UUID seasonId,
		@Valid @RequestPart("request") EpisodeCreateRequest request,
		@RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail
	) {
		EpisodeResponse response = contentCommandService.createEpisode(seasonId, request, thumbnail);
		return ResponseEntity
			.created(URI.create("/api/contents/" + seasonId + "/episodes/" + response.getId()))
			.body(response);
	}

	@PatchMapping(value = "/{seasonId}/episodes/{episodeId}", consumes = "multipart/form-data")
	public ResponseEntity<EpisodeResponse> updateEpisode(
		@PathVariable UUID seasonId,
		@PathVariable UUID episodeId,
		@Valid @RequestPart("request") EpisodeUpdateRequest request,
		@RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail
	) {
		return ResponseEntity.ok(
			contentCommandService.updateEpisode(seasonId, episodeId, request, thumbnail));
	}

	@DeleteMapping("/{seasonId}/episodes/{episodeId}")
	public ResponseEntity<Void> deleteEpisode(
		@PathVariable UUID seasonId,
		@PathVariable UUID episodeId
	) {
		contentCommandService.deleteEpisode(seasonId, episodeId);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{contentId}")
	public ResponseEntity<Void> delete(@PathVariable UUID contentId) {
		contentCommandService.delete(contentId);
		return ResponseEntity.noContent().build();
	}
}
