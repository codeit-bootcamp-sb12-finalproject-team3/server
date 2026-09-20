package com.moduplaylist.api.content.controller;

import com.moduplaylist.api.content.dto.ContentSearchRequest;
import com.moduplaylist.api.content.dto.ContentSummaryResponse;
import com.moduplaylist.api.content.dto.ContentTypeFilter;
import com.moduplaylist.api.content.dto.GenreResponse;
import com.moduplaylist.api.content.dto.SportTypeResponse;
import com.moduplaylist.api.content.dto.ContentResponse;
import com.moduplaylist.api.content.dto.ContentPlatformResponse;
import com.moduplaylist.api.content.dto.ContentPlaylistResponse;
import com.moduplaylist.api.content.dto.ContentWatchPartyResponse;
import com.moduplaylist.api.content.service.ContentQueryService;
import com.moduplaylist.api.global.dto.CursorPageResponse;
import com.moduplaylist.api.global.security.CustomUserDetails;
import com.moduplaylist.core.content.exception.InvalidContentSearchException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/contents")
public class ContentQueryController {

	private static final Set<String> ALLOWED_SEARCH_PARAMETERS = Set.of(
		"keywordLike",
		"typeEqual",
		"genreIdEqual",
		"sportTypeEqual",
		"likedByUserIdEqual",
		"sortBy",
		"cursor",
		"idAfter",
		"limit"
	);

	private final ContentQueryService contentQueryService;

	@GetMapping
	public ResponseEntity<CursorPageResponse<ContentSummaryResponse>> findAll(
		@Valid @ModelAttribute ContentSearchRequest request,
		HttpServletRequest httpRequest
	) {
		validateSearchParameters(httpRequest);
		return ResponseEntity.ok(contentQueryService.findAll(request));
	}

	@GetMapping("/genres")
	public ResponseEntity<List<GenreResponse>> findGenres(
		@RequestParam ContentTypeFilter type
	) {
		return ResponseEntity.ok(contentQueryService.findGenres(type));
	}

	@GetMapping("/sport-types")
	public ResponseEntity<List<SportTypeResponse>> findSportTypes() {
		return ResponseEntity.ok(contentQueryService.findSportTypes());
	}

	@GetMapping("/{contentId}")
	public ResponseEntity<ContentResponse> findById(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@PathVariable UUID contentId
	) {
		return ResponseEntity.ok(contentQueryService.findById(userDetails.getUserId(), contentId));
	}

	@GetMapping("/{contentId}/ott")
	public ResponseEntity<ContentPlatformResponse> findOtt(
		@PathVariable UUID contentId
	) {
		return ResponseEntity.ok(contentQueryService.findOtt(contentId));
	}

	@GetMapping("/{contentId}/playlists")
	public ResponseEntity<ContentPlaylistResponse> findPlaylists(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@PathVariable UUID contentId
	) {
		return ResponseEntity.ok(contentQueryService.findPlaylists(userDetails.getUserId(), contentId));
	}

	@GetMapping("/{contentId}/watch-parties")
	public ResponseEntity<ContentWatchPartyResponse> findWatchParties(@PathVariable UUID contentId) {
		return ResponseEntity.ok(contentQueryService.findWatchParties(contentId));
	}

	private void validateSearchParameters(HttpServletRequest request) {
		boolean hasUnknownParameter = request.getParameterMap().keySet().stream()
			.anyMatch(parameter -> !ALLOWED_SEARCH_PARAMETERS.contains(parameter));
		if (hasUnknownParameter) {
			throw new InvalidContentSearchException();
		}
	}
}
