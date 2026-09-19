package com.moduplaylist.api.content.service;

import com.moduplaylist.api.content.dto.ContentSearchRequest;
import com.moduplaylist.api.content.dto.ContentSummaryResponse;
import com.moduplaylist.api.content.dto.ContentResponse;
import com.moduplaylist.api.content.dto.ContentPlatformResponse;
import com.moduplaylist.api.content.dto.ContentPlaylistResponse;
import com.moduplaylist.api.content.dto.ContentWatchPartyResponse;
import com.moduplaylist.api.content.dto.ContentAutocompleteRequest;
import com.moduplaylist.api.content.dto.ContentAutocompleteResponse;
import com.moduplaylist.api.content.dto.ContentTypeFilter;
import com.moduplaylist.api.content.dto.GenreResponse;
import com.moduplaylist.api.content.dto.SportTypeResponse;
import com.moduplaylist.api.content.dto.PlatformResponse;
import com.moduplaylist.api.content.dto.ContentSeriesSearchResponse;
import com.moduplaylist.api.global.dto.CursorPageResponse;
import java.util.List;
import java.util.UUID;

public interface ContentQueryService {

	CursorPageResponse<ContentSummaryResponse> findAll(UUID userId, ContentSearchRequest request);

	List<GenreResponse> findGenres(ContentTypeFilter type);

	List<SportTypeResponse> findSportTypes();

	List<PlatformResponse> findPlatforms();

	ContentSeriesSearchResponse searchSeriesForAdmin(String query);

	ContentResponse findById(UUID userId, UUID contentId);

	ContentResponse findByIdForCommand(UUID contentId);

	ContentPlatformResponse findOtt(UUID contentId);

	ContentPlaylistResponse findPlaylists(UUID userId, UUID contentId);

	ContentWatchPartyResponse findWatchParties(UUID contentId);

	ContentAutocompleteResponse autocomplete(UUID userId, ContentAutocompleteRequest request);
}
