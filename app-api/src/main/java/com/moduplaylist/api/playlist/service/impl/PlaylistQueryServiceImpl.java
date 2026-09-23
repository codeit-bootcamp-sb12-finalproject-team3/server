package com.moduplaylist.api.playlist.service.impl;

import com.moduplaylist.api.content.dto.ContentSummary;
import com.moduplaylist.api.global.dto.CursorPageResponse;
import com.moduplaylist.api.global.dto.SortDirection;
import com.moduplaylist.api.playlist.dto.PlaylistResponse;
import com.moduplaylist.api.playlist.dto.PlaylistSummaryResponse;
import com.moduplaylist.api.playlist.service.PlaylistQueryService;
import com.moduplaylist.api.playlist.service.PlaylistResponseAssembler;
import com.moduplaylist.api.user.dto.UserSummary;
import com.moduplaylist.core.playlist.entity.Playlist;
import com.moduplaylist.core.playlist.entity.PlaylistContent;
import com.moduplaylist.core.playlist.entity.PlaylistSubscription;
import com.moduplaylist.core.playlist.exception.PlaylistNotFoundException;
import com.moduplaylist.core.playlist.repository.PlaylistContentQueryRepository;
import com.moduplaylist.core.playlist.repository.PlaylistQueryRepository;
import com.moduplaylist.core.playlist.repository.PlaylistRepository;
import com.moduplaylist.core.playlist.repository.PlaylistSearch;
import com.moduplaylist.core.playlist.repository.PlaylistSubscriptionRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlaylistQueryServiceImpl implements PlaylistQueryService {

  private static final int PREVIEW_CONTENT_LIMIT = 4;

  private final PlaylistRepository playlistRepository;
  private final PlaylistSubscriptionRepository playlistSubscriptionRepository;
  private final PlaylistQueryRepository playlistQueryRepository;
  private final PlaylistContentQueryRepository playlistContentQueryRepository;
  private final PlaylistResponseAssembler playlistResponseAssembler;

  @Override
  @Transactional(readOnly = true)
  public PlaylistResponse findById(UUID userId, UUID playlistId) {
    Playlist playlist = playlistRepository.findById(playlistId)
        .orElseThrow(() -> new PlaylistNotFoundException(playlistId));

    return playlistResponseAssembler.toResponse(playlist, userId);
  }

  @Override
  @Transactional(readOnly = true)
  public CursorPageResponse<PlaylistSummaryResponse> findAll(
      UUID userId,
      PlaylistSearch search
  ) {
    PlaylistQueryRepository.SearchResult result =
        playlistQueryRepository.search(search);

    List<PlaylistQueryRepository.Item> items = result.getItems();

    if (items.isEmpty()) {
      return CursorPageResponse.<PlaylistSummaryResponse>builder()
          .data(List.of())
          .nextCursor(null)
          .nextIdAfter(null)
          .hasNext(false)
          .totalCount(result.getTotalCount())
          .sortBy(toSortBy(search.getSort()))
          .sortDirection(toSortDirection(search.getDirection()))
          .build();
    }

    List<UUID> playlistIds = items.stream()
        .map(PlaylistQueryRepository.Item::getPlaylist)
        .map(Playlist::getId)
        .toList();

    Set<UUID> subscribedPlaylistIds =
        findSubscribedPlaylistIds(userId, playlistIds);

    Map<UUID, List<ContentSummary>> previewContentsByPlaylistId =
        findPreviewContentsByPlaylistId(playlistIds);

    List<PlaylistSummaryResponse> data = items.stream()
        .map(item -> toSummaryResponse(
            item,
            subscribedPlaylistIds,
            previewContentsByPlaylistId
        ))
        .toList();

    Playlist lastPlaylist = items.get(items.size() - 1).getPlaylist();

    String nextCursor =
        result.isHasNext()
            ? getCursorValue(lastPlaylist, search.getSort())
            : null;

    UUID nextIdAfter =
        result.isHasNext()
            ? lastPlaylist.getId()
            : null;

    return CursorPageResponse.<PlaylistSummaryResponse>builder()
        .data(data)
        .nextCursor(nextCursor)
        .nextIdAfter(nextIdAfter)
        .hasNext(result.isHasNext())
        .totalCount(result.getTotalCount())
        .sortBy(toSortBy(search.getSort()))
        .sortDirection(toSortDirection(search.getDirection()))
        .build();
  }

  private Set<UUID> findSubscribedPlaylistIds(
      UUID userId,
      List<UUID> playlistIds
  ) {
    List<PlaylistSubscription> subscriptions =
        playlistSubscriptionRepository.findAllByUser_IdAndPlaylist_IdIn(
            userId,
            playlistIds
        );

    Set<UUID> subscribedPlaylistIds = new HashSet<>();

    for (PlaylistSubscription subscription : subscriptions) {
      subscribedPlaylistIds.add(subscription.getPlaylist().getId());
    }

    return subscribedPlaylistIds;
  }

  private Map<UUID, List<ContentSummary>> findPreviewContentsByPlaylistId(
      List<UUID> playlistIds
  ) {
    List<PlaylistContent> previewContents =
        playlistContentQueryRepository.findPreviewContents(
            playlistIds,
            PREVIEW_CONTENT_LIMIT
        );

    Map<UUID, List<ContentSummary>> result = new HashMap<>();

    for (PlaylistContent playlistContent : previewContents) {
      UUID playlistId = playlistContent.getPlaylist().getId();

      result.computeIfAbsent(
              playlistId,
              key -> new ArrayList<>()
          )
          .add(ContentSummary.from(playlistContent.getContent()));
    }

    return result;
  }

  private PlaylistSummaryResponse toSummaryResponse(
      PlaylistQueryRepository.Item item,
      Set<UUID> subscribedPlaylistIds,
      Map<UUID, List<ContentSummary>> previewContentsByPlaylistId
  ) {
    Playlist playlist = item.getPlaylist();

    return PlaylistSummaryResponse.builder()
        .id(playlist.getId())
        .owner(UserSummary.from(playlist.getOwner()))
        .title(playlist.getTitle())
        .description(playlist.getDescription())
        .createdAt(playlist.getCreatedAt())
        .subscriberCount(item.getSubscriberCount())
        .subscribedByMe(
            subscribedPlaylistIds.contains(playlist.getId())
        )
        .contentCount(item.getContentCount())
        .previewContents(
            previewContentsByPlaylistId.getOrDefault(
                playlist.getId(),
                List.of()
            )
        )
        .build();
  }

  private String getCursorValue(
      Playlist playlist,
      PlaylistSearch.Sort sort
  ) {
    return switch (sort) {
      case CREATED_AT ->
          playlist.getCreatedAt().toString();

      case WEEKLY_POPULARITY_SCORE ->
          playlist.getWeeklyPopularityScore().toPlainString();
    };
  }

  private String toSortBy(PlaylistSearch.Sort sort) {
    return switch (sort) {
      case CREATED_AT -> "createdAt";
      case WEEKLY_POPULARITY_SCORE -> "weeklyPopularityScore";
    };
  }

  private SortDirection toSortDirection(
      PlaylistSearch.Direction direction
  ) {
    return switch (direction) {
      case ASCENDING -> SortDirection.ASCENDING;
      case DESCENDING -> SortDirection.DESCENDING;
    };
  }
}
