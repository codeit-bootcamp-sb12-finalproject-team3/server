package com.moduplaylist.api.content.controller;

import com.moduplaylist.api.content.dto.ContentSearchRequest;
import com.moduplaylist.api.content.dto.ContentSummaryResponse;
import com.moduplaylist.api.content.dto.ContentTypeFilter;
import com.moduplaylist.api.content.dto.GenreResponse;
import com.moduplaylist.api.content.dto.SportTypeResponse;
import com.moduplaylist.api.content.dto.PlatformResponse;
import com.moduplaylist.api.content.dto.ContentResponse;
import com.moduplaylist.api.content.dto.ContentPlatformResponse;
import com.moduplaylist.api.content.dto.ContentPlaylistResponse;
import com.moduplaylist.api.content.dto.ContentWatchPartyResponse;
import com.moduplaylist.api.content.dto.EpisodeResponse;
import com.moduplaylist.api.content.service.ContentQueryService;
import com.moduplaylist.api.content.service.NewContentService;
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
		"likedByMe",
		"likedByUserIdEqual",
		"sortBy",
		"cursor",
		"idAfter",
		"limit"
	);
	private static final Set<String> ALLOWED_NEW_CONTENT_PARAMETERS = Set.of(
		"cursor",
		"idAfter",
		"limit"
	);

	private final ContentQueryService contentQueryService;
	private final NewContentService newContentService;

	@GetMapping
	public ResponseEntity<CursorPageResponse<ContentSummaryResponse>> findAll(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@Valid @ModelAttribute ContentSearchRequest request,
		HttpServletRequest httpRequest
	) {
		validateSearchParameters(httpRequest);
		return ResponseEntity.ok(contentQueryService.findAll(userDetails.getUserId(), request));
	}

	@GetMapping("/new")
	public ResponseEntity<CursorPageResponse<ContentSummaryResponse>> findNewContents(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@RequestParam(required = false) String cursor,
		@RequestParam(required = false) UUID idAfter,
		@RequestParam(defaultValue = "20") int limit,
		HttpServletRequest httpRequest
	) {
		validateParameters(httpRequest, ALLOWED_NEW_CONTENT_PARAMETERS);
		return ResponseEntity.ok(
			newContentService.findNewContents(
				userDetails.getUserId(),
				cursor,
				idAfter,
				limit
			)
		);
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

	@GetMapping("/platforms")
	public ResponseEntity<List<PlatformResponse>> findPlatforms() {
		return ResponseEntity.ok(contentQueryService.findPlatforms());
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

	@GetMapping("/{seasonId}/episodes")
	public ResponseEntity<List<EpisodeResponse>> findEpisodes(
		@PathVariable UUID seasonId
	) {
		return ResponseEntity.ok(contentQueryService.findEpisodes(seasonId));
	}

	@GetMapping("/{seasonId}/episodes/{episodeId}")
	public ResponseEntity<EpisodeResponse> findEpisode(
		@PathVariable UUID seasonId,
		@PathVariable UUID episodeId
	) {
		return ResponseEntity.ok(contentQueryService.findEpisode(seasonId, episodeId));
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
		validateParameters(request, ALLOWED_SEARCH_PARAMETERS);
	}

	private void validateParameters(HttpServletRequest request, Set<String> allowedParameters) {
		boolean hasUnknownParameter = request.getParameterMap().keySet().stream()
			.anyMatch(parameter -> !allowedParameters.contains(parameter));
		if (hasUnknownParameter) {
			throw new InvalidContentSearchException();
		}
	}
}
