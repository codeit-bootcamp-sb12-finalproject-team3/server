package com.moduplaylist.api.content.controller;

import com.moduplaylist.api.content.dto.ContentLikeResponseDto;
import com.moduplaylist.api.content.service.ContentLikeService;
import com.moduplaylist.api.global.security.CustomUserDetails;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/contents/{contentId}/likes")
public class ContentLikeController {

	private final ContentLikeService contentLikeService;

	@GetMapping
	public ResponseEntity<ContentLikeResponseDto> get(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@PathVariable UUID contentId
	) {
		return ResponseEntity.ok(contentLikeService.get(userDetails.getUserId(), contentId));
	}

	@PutMapping
	public ResponseEntity<ContentLikeResponseDto> like(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@PathVariable UUID contentId
	) {
		return ResponseEntity.ok(contentLikeService.like(userDetails.getUserId(), contentId));
	}

	@DeleteMapping
	public ResponseEntity<ContentLikeResponseDto> unlike(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@PathVariable UUID contentId
	) {
		return ResponseEntity.ok(contentLikeService.unlike(userDetails.getUserId(), contentId));
	}
}
