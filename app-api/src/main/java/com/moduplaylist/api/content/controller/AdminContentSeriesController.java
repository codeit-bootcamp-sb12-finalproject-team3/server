package com.moduplaylist.api.content.controller;

import com.moduplaylist.api.content.dto.ContentSeriesSearchResponse;
import com.moduplaylist.api.content.service.ContentQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/content-series")
@PreAuthorize("hasRole('ADMIN')")
public class AdminContentSeriesController {

	private final ContentQueryService contentQueryService;

	@GetMapping
	public ResponseEntity<ContentSeriesSearchResponse> search(
		@RequestParam String query
	) {
		return ResponseEntity.ok(contentQueryService.searchSeriesForAdmin(query));
	}
}
