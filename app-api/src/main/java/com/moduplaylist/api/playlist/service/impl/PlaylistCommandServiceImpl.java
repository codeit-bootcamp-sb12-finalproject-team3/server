package com.moduplaylist.api.playlist.service.impl;

import com.moduplaylist.api.playlist.dto.PlaylistCreateRequest;
import com.moduplaylist.api.playlist.dto.PlaylistResponse;
import com.moduplaylist.api.playlist.dto.PlaylistUpdateRequest;
import com.moduplaylist.api.playlist.event.PlaylistTagRecalculationEvent;
import com.moduplaylist.api.playlist.service.PlaylistCommandService;
import com.moduplaylist.api.playlist.service.PlaylistResponseAssembler;
import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.exception.ContentNotFoundException;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.core.playlist.entity.Playlist;
import com.moduplaylist.core.playlist.entity.PlaylistContent;
import com.moduplaylist.core.playlist.exception.InvalidPlaylistContentRequestException;
import com.moduplaylist.core.playlist.exception.PlaylistAccessDeniedException;
import com.moduplaylist.core.playlist.exception.PlaylistNotFoundException;
import com.moduplaylist.core.playlist.policy.PlaylistContentPolicy;
import com.moduplaylist.core.playlist.repository.PlaylistContentRepository;
import com.moduplaylist.core.playlist.repository.PlaylistRepository;
import com.moduplaylist.core.user.entity.User;
import com.moduplaylist.core.user.exception.UserNotFoundException;
import com.moduplaylist.core.user.repository.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlaylistCommandServiceImpl implements PlaylistCommandService {

  private final PlaylistRepository playlistRepository;
  private final UserRepository userRepository;
  private final ContentRepository contentRepository;
  private final PlaylistContentRepository playlistContentRepository;
  private final PlaylistResponseAssembler playlistResponseAssembler;
  private final ApplicationEventPublisher eventPublisher;

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

    PlaylistContentPolicy.validateMinimumContentCount(uniqueContentIds.size());

    List<Content> contents = contentRepository.findAllById(uniqueContentIds);

    if (contents.size() != uniqueContentIds.size()) {
      Set<UUID> foundContentIds = contents.stream()
          .map(Content::getId)
          .collect(Collectors.toSet());

      UUID missingContentId = uniqueContentIds.stream()
          .filter(contentId -> !foundContentIds.contains(contentId))
          .findFirst()
          .orElseThrow();

      throw new ContentNotFoundException(missingContentId);
    }

    contents.forEach(PlaylistContentPolicy::validateContent);

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

    eventPublisher.publishEvent(
        new PlaylistTagRecalculationEvent(savedPlaylist.getId())
    );

    return playlistResponseAssembler.toResponse(savedPlaylist, userId);
  }

  @Override
  @Transactional
  public PlaylistResponse update(
      UUID userId,
      UUID playlistId,
      PlaylistUpdateRequest request
  ) {
    Playlist playlist = playlistRepository.findById(playlistId)
        .orElseThrow(() -> new PlaylistNotFoundException(playlistId));

    if (!playlist.getOwner().getId().equals(userId)) {
      throw new PlaylistAccessDeniedException(playlistId);
    }

    playlist.update(request.getTitle(), request.getDescription());

    return playlistResponseAssembler.toResponse(playlist, userId);
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
}
