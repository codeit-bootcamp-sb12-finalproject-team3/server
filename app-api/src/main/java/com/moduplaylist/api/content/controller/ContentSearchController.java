package com.moduplaylist.api.content.controller;

import com.moduplaylist.api.content.dto.ContentAutocompleteRequest;
import com.moduplaylist.api.content.dto.ContentAutocompleteResponse;
import com.moduplaylist.api.content.service.ContentQueryService;
import com.moduplaylist.api.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search/contents")
public class ContentSearchController {

	private final ContentQueryService contentQueryService;

	@GetMapping("/autocomplete")
	public ResponseEntity<ContentAutocompleteResponse> autocomplete(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@Valid @ModelAttribute ContentAutocompleteRequest request
	) {
		return ResponseEntity.ok(contentQueryService.autocomplete(userDetails.getUserId(), request));
	}
}
