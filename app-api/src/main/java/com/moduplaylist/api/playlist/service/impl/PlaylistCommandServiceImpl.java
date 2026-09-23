package com.moduplaylist.api.playlist.service.impl;

import com.moduplaylist.api.playlist.dto.PlaylistCreateRequest;
import com.moduplaylist.api.playlist.dto.PlaylistResponse;
import com.moduplaylist.api.playlist.dto.PlaylistUpdateRequest;
import com.moduplaylist.api.playlist.event.PlaylistTagRecalculationEvent;
import com.moduplaylist.api.playlist.service.PlaylistCommandService;
import com.moduplaylist.api.playlist.service.PlaylistResponseAssembler;
import com.moduplaylist.core.playlist.entity.Playlist;
import com.moduplaylist.core.playlist.exception.PlaylistAccessDeniedException;
import com.moduplaylist.core.playlist.exception.PlaylistNotFoundException;
import com.moduplaylist.core.playlist.repository.PlaylistRepository;
import com.moduplaylist.core.playlist.service.PlaylistCreationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlaylistCommandServiceImpl implements PlaylistCommandService {

  private final PlaylistRepository playlistRepository;
  private final PlaylistResponseAssembler playlistResponseAssembler;
  private final ApplicationEventPublisher eventPublisher;
  private final PlaylistCreationService playlistCreationService;

  @Override
  @Transactional
  public PlaylistResponse create(UUID userId, PlaylistCreateRequest request) {
    Playlist savedPlaylist = playlistCreationService.create(
        userId,
        request.getTitle(),
        request.getDescription(),
        request.getContentIds()
    );

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
