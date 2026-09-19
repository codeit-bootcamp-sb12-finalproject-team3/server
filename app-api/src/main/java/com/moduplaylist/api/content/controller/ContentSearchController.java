package com.moduplaylist.api.content.controller;

import com.moduplaylist.api.content.dto.ContentAutocompleteRequest;
import com.moduplaylist.api.content.dto.ContentAutocompleteResponse;
import com.moduplaylist.api.content.service.ContentQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
		@Valid @ModelAttribute ContentAutocompleteRequest request
	) {
		return ResponseEntity.ok(contentQueryService.autocomplete(request));
	}
}
