package com.moduplaylist.api.playlist.service.impl;

import com.moduplaylist.api.content.dto.ContentSummary;
import com.moduplaylist.api.global.dto.CursorPageResponse;
import com.moduplaylist.api.global.dto.SortDirection;
import com.moduplaylist.api.playlist.dto.PlaylistCreateRequest;
import com.moduplaylist.api.playlist.dto.PlaylistResponse;
import com.moduplaylist.api.playlist.dto.PlaylistSummaryResponse;
import com.moduplaylist.api.playlist.dto.PlaylistUpdateRequest;
import com.moduplaylist.api.playlist.service.PlaylistService;
import com.moduplaylist.api.user.dto.UserSummary;
import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.exception.ContentNotFoundException;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.core.playlist.entity.Playlist;
import com.moduplaylist.core.playlist.entity.PlaylistContent;
import com.moduplaylist.core.playlist.entity.PlaylistSubscription;
import com.moduplaylist.core.playlist.exception.InvalidPlaylistContentRequestException;
import com.moduplaylist.core.playlist.exception.PlaylistAccessDeniedException;
import com.moduplaylist.core.playlist.exception.PlaylistAlreadySubscribedException;
import com.moduplaylist.core.playlist.exception.PlaylistContentAlreadyExistsException;
import com.moduplaylist.core.playlist.exception.PlaylistContentNotFoundException;
import com.moduplaylist.core.playlist.exception.PlaylistMinimumContentException;
import com.moduplaylist.core.playlist.exception.PlaylistNotFoundException;
import com.moduplaylist.core.playlist.exception.PlaylistSubscriptionNotFoundException;
import com.moduplaylist.core.playlist.exception.SelfPlaylistSubscriptionNotAllowedException;
import com.moduplaylist.core.playlist.repository.PlaylistContentQueryRepository;
import com.moduplaylist.core.playlist.repository.PlaylistContentRepository;
import com.moduplaylist.core.playlist.repository.PlaylistQueryRepository;
import com.moduplaylist.core.playlist.repository.PlaylistRepository;
import com.moduplaylist.core.playlist.repository.PlaylistSearch;
import com.moduplaylist.core.playlist.repository.PlaylistSubscriptionRepository;
import com.moduplaylist.core.user.entity.User;
import com.moduplaylist.core.user.exception.UserNotFoundException;
import com.moduplaylist.core.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlaylistServiceImpl implements PlaylistService {

  private static final int MIN_CONTENT_COUNT = 4;
  private static final int PREVIEW_CONTENT_LIMIT = 4;

  private final PlaylistRepository playlistRepository;
  private final UserRepository userRepository;
  private final ContentRepository contentRepository;
  private final PlaylistContentRepository playlistContentRepository;
  private final PlaylistSubscriptionRepository playlistSubscriptionRepository;
  private final PlaylistQueryRepository playlistQueryRepository;
  private final PlaylistContentQueryRepository playlistContentQueryRepository;

  @Override
  @Transactional
  public PlaylistResponse create(UUID userId, PlaylistCreateRequest request) {
    User owner = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));

    List<UUID> contentIds = request.getContentIds();
    Set<UUID> uniqueContentIds = new HashSet<>(contentIds);

    if (uniqueContentIds.size() != contentIds.size()) {
      throw new InvalidPlaylistContentRequestException();
    }

    if (uniqueContentIds.size() < MIN_CONTENT_COUNT) {
      throw new PlaylistMinimumContentException();
    }

    List<Content> contents = contentRepository.findAllById(uniqueContentIds);

    if (contents.size() != uniqueContentIds.size()) {
      // 요청한 contentId 중 존재하지 않는 콘텐츠 ID 확인
      Set<UUID> foundContentIds = contents.stream()
          .map(Content::getId)
          .collect(Collectors.toSet());

      UUID missingContentId = uniqueContentIds.stream()
          .filter(contentId -> !foundContentIds.contains(contentId))
          .findFirst()
          .orElseThrow();

      throw new ContentNotFoundException(missingContentId);
    }

    Playlist playlist = Playlist.create(
        owner,
        request.getTitle(),
        request.getDescription()
    );

    Playlist savedPlaylist = playlistRepository.save(playlist);

    List<PlaylistContent> playlistContents = contents.stream()
        .map(content -> PlaylistContent.create(savedPlaylist, content))
        .toList();

    playlistContentRepository.saveAll(playlistContents);

    return PlaylistResponse.builder()
        .id(savedPlaylist.getId())
        .owner(UserSummary.from(owner))
        .title(savedPlaylist.getTitle())
        .description(savedPlaylist.getDescription())
        .updatedAt(savedPlaylist.getUpdatedAt())
        .subscriberCount(0L)
        .subscribedByMe(false)
        .contents(
            contents.stream()
                .map(ContentSummary::from)
                .toList()
        )
        .build();
  }

  @Override
  @Transactional(readOnly = true)
  public PlaylistResponse findById(UUID userId, UUID playlistId) {
    Playlist playlist = playlistRepository.findById(playlistId)
        .orElseThrow(() -> new PlaylistNotFoundException(playlistId));

    return toResponse(playlist, userId);
  }

  @Override
  @Transactional(readOnly = true)
  public CursorPageResponse<PlaylistSummaryResponse> findAll(UUID userId, PlaylistSearch search) {
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

    String nextCursor = result.isHasNext() ? getCursorValue(lastPlaylist, search.getSort()) : null;

    UUID nextIdAfter = result.isHasNext() ? lastPlaylist.getId() : null;

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

  @Override
  @Transactional
  public PlaylistResponse update(UUID userId, UUID playlistId, PlaylistUpdateRequest request) {
    Playlist playlist = playlistRepository.findById(playlistId)
        .orElseThrow(() -> new PlaylistNotFoundException(playlistId));

    if (!playlist.getOwner().getId().equals(userId)) {
      throw new PlaylistAccessDeniedException(playlistId);
    }

    playlist.update(request.getTitle(), request.getDescription());

    return toResponse(playlist, userId);
  }

  @Override
  @Transactional
  public void delete(UUID userId, UUID playlistId) {
    Playlist playlist = playlistRepository.findById(playlistId)
        .orElseThrow(() -> new PlaylistNotFoundException(playlistId));

    if (!playlist.getOwner().getId().equals(userId)) {
      throw new PlaylistAccessDeniedException(playlistId);
    }

    playlistRepository.delete(playlist);
  }

  @Override
  @Transactional
  public void subscribe(UUID userId, UUID playlistId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));

    Playlist playlist = playlistRepository.findById(playlistId)
        .orElseThrow(() -> new PlaylistNotFoundException(playlistId));

    if (playlist.getOwner().getId().equals(userId)) {
      throw new SelfPlaylistSubscriptionNotAllowedException(userId, playlistId);
    }

    if (playlistSubscriptionRepository.existsByUser_IdAndPlaylist_Id(userId, playlistId)) {
      throw new PlaylistAlreadySubscribedException(userId, playlistId);
    }

    PlaylistSubscription subscription = PlaylistSubscription.create(user, playlist);

    try {
      playlistSubscriptionRepository.saveAndFlush(subscription);
    } catch (DataIntegrityViolationException e) {
      throw new PlaylistAlreadySubscribedException(userId, playlistId, e);
    }
  }

  @Override
  @Transactional
  public void unsubscribe(UUID userId, UUID playlistId) {
    playlistRepository.findById(playlistId)
        .orElseThrow(() -> new PlaylistNotFoundException(playlistId));

    PlaylistSubscription subscription =
        playlistSubscriptionRepository
            .findByUser_IdAndPlaylist_Id(userId, playlistId)
            .orElseThrow(() -> new PlaylistSubscriptionNotFoundException(userId, playlistId));

    playlistSubscriptionRepository.delete(subscription);
  }

  private Set<UUID> findSubscribedPlaylistIds(
      UUID userId,
      List<UUID> playlistIds
  ) {
    List<PlaylistSubscription> subscriptions =
        playlistSubscriptionRepository.findAllByUser_IdAndPlaylist_IdIn(userId, playlistIds);

    Set<UUID> subscribedPlaylistIds = new HashSet<>();

    for (PlaylistSubscription subscription : subscriptions) {
      subscribedPlaylistIds.add(subscription.getPlaylist().getId());
    }

    return subscribedPlaylistIds;
  }

  @Override
  @Transactional
  public void addContent(UUID userId, UUID playlistId, UUID contentId) {
    Playlist playlist = playlistRepository.findById(playlistId)
        .orElseThrow(() -> new PlaylistNotFoundException(playlistId));

    if (!playlist.getOwner().getId().equals(userId)) {
      throw new PlaylistAccessDeniedException(playlistId);
    }

    Content content = contentRepository.findById(contentId)
        .orElseThrow(() -> new ContentNotFoundException(contentId));

    if (playlistContentRepository.existsByPlaylist_IdAndContent_Id(
        playlistId,
        contentId
    )) {
      throw new PlaylistContentAlreadyExistsException(playlistId, contentId);
    }

    PlaylistContent playlistContent = PlaylistContent.create(playlist, content);

    try {
      playlistContentRepository.saveAndFlush(playlistContent);
    } catch (DataIntegrityViolationException e) {
      throw new PlaylistContentAlreadyExistsException(
          playlistId,
          contentId,
          e
      );
    }
  }

  @Override
  @Transactional
  public void removeContent(UUID userId, UUID playlistId, UUID contentId) {
    Playlist playlist = playlistRepository.findByIdForUpdate(playlistId)
        .orElseThrow(() -> new PlaylistNotFoundException(playlistId));

    if (!playlist.getOwner().getId().equals(userId)) {
      throw new PlaylistAccessDeniedException(playlistId);
    }

    contentRepository.findById(contentId)
        .orElseThrow(() -> new ContentNotFoundException(contentId));

    PlaylistContent playlistContent = playlistContentRepository
        .findByPlaylist_IdAndContent_Id(playlistId, contentId)
        .orElseThrow(
            () -> new PlaylistContentNotFoundException(playlistId, contentId)
        );

    long contentCount = playlistContentRepository.countByPlaylist_Id(playlistId);

    if (contentCount <= MIN_CONTENT_COUNT) {
      throw new PlaylistMinimumContentException();
    }

    playlistContentRepository.delete(playlistContent);
  }

  private Map<UUID, List<ContentSummary>> findPreviewContentsByPlaylistId(List<UUID> playlistIds) {
    List<PlaylistContent> previewContents =
        playlistContentQueryRepository.findPreviewContents(
            playlistIds,
            PREVIEW_CONTENT_LIMIT
        );

    Map<UUID, List<ContentSummary>> result = new HashMap<>();

    for (PlaylistContent playlistContent: previewContents) {
      UUID playlistId = playlistContent.getPlaylist().getId();

      result.computeIfAbsent(
        playlistId,
        key -> new ArrayList<>())
        .add(
            ContentSummary.from(playlistContent.getContent())
      );
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

  private SortDirection toSortDirection(PlaylistSearch.Direction direction) {
    return switch (direction) {
      case ASCENDING -> SortDirection.ASCENDING;
      case DESCENDING -> SortDirection.DESCENDING;
    };
  }

  private PlaylistResponse toResponse(Playlist playlist, UUID userId) {
    long subscriberCount = playlistSubscriptionRepository.countByPlaylist_Id(playlist.getId());

    boolean subscribedByMe = playlistSubscriptionRepository.existsByUser_IdAndPlaylist_Id(
        userId,
        playlist.getId()
    );

    List<ContentSummary> contents =
        playlistContentRepository
            .findAllByPlaylist_IdOrderByCreatedAtAscIdAsc(playlist.getId())
            .stream()
            .map(PlaylistContent::getContent)
            .map(ContentSummary::from)
            .toList();

    return PlaylistResponse.builder()
        .id(playlist.getId())
        .owner(UserSummary.from(playlist.getOwner()))
        .title(playlist.getTitle())
        .description(playlist.getDescription())
        .updatedAt(playlist.getUpdatedAt())
        .subscriberCount(subscriberCount)
        .subscribedByMe(subscribedByMe)
        .contents(contents)
        .build();
  }
}
