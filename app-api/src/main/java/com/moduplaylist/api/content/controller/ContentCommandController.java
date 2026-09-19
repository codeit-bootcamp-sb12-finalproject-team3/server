package com.moduplaylist.api.content.controller;

import com.moduplaylist.api.content.dto.ContentCreateRequest;
import com.moduplaylist.api.content.dto.ContentCreateResponse;
import com.moduplaylist.api.content.dto.ContentResponse;
import com.moduplaylist.api.content.dto.ContentUpdateRequest;
import com.moduplaylist.api.content.service.ContentCommandService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
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
		@RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail
	) {
		ContentCreateResponse response = contentCommandService.create(request, thumbnail);
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

	@DeleteMapping("/{contentId}")
	public ResponseEntity<Void> delete(@PathVariable UUID contentId) {
		contentCommandService.delete(contentId);
		return ResponseEntity.noContent().build();
	}
}
