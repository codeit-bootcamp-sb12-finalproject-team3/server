package com.moduplaylist.api.content.controller;

import com.moduplaylist.api.content.dto.ContentResponse;
import com.moduplaylist.api.content.dto.ContentPlatformResponse;
import com.moduplaylist.api.content.dto.ContentUpdateRequest;
import com.moduplaylist.api.content.service.ContentCommandService;
import com.moduplaylist.api.content.service.ContentQueryService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/content-seasons")
@PreAuthorize("hasRole('ADMIN')")
public class AdminContentSeasonController {

	private final ContentQueryService contentQueryService;
	private final ContentCommandService contentCommandService;

	@GetMapping("/{hiddenSeasonId}")
	public ResponseEntity<ContentResponse> findHiddenSeason(
		@PathVariable UUID hiddenSeasonId
	) {
		return ResponseEntity.ok(
			contentQueryService.findHiddenSeasonByIdForAdmin(hiddenSeasonId));
	}

	@GetMapping("/{hiddenSeasonId}/ott")
	public ResponseEntity<ContentPlatformResponse> findHiddenSeasonOtt(
		@PathVariable UUID hiddenSeasonId
	) {
		return ResponseEntity.ok(
			contentQueryService.findHiddenSeasonOttByIdForAdmin(hiddenSeasonId));
	}

	@PatchMapping(value = "/{hiddenSeasonId}/restore", consumes = "multipart/form-data")
	public ResponseEntity<ContentResponse> restoreSeason(
		@PathVariable UUID hiddenSeasonId,
		@Valid @RequestPart("request") ContentUpdateRequest request,
		@RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail
	) {
		return ResponseEntity.ok(
			contentCommandService.restoreSeason(hiddenSeasonId, request, thumbnail));
	}
}
