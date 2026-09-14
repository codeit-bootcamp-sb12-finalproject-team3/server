package com.moduplaylist.api.playlist.service.impl;

import com.moduplaylist.api.content.dto.ContentSummary;
import com.moduplaylist.api.playlist.dto.PlaylistCreateRequest;
import com.moduplaylist.api.playlist.dto.PlaylistResponse;
import com.moduplaylist.api.playlist.dto.PlaylistUpdateRequest;
import com.moduplaylist.api.playlist.service.PlaylistService;
import com.moduplaylist.api.user.dto.UserSummary;
import com.moduplaylist.core.playlist.entity.Playlist;
import com.moduplaylist.core.playlist.entity.PlaylistContent;
import com.moduplaylist.core.playlist.exception.PlaylistAccessDeniedException;
import com.moduplaylist.core.playlist.exception.PlaylistNotFoundException;
import com.moduplaylist.core.playlist.repository.PlaylistContentRepository;
import com.moduplaylist.core.playlist.repository.PlaylistRepository;
import com.moduplaylist.core.playlist.repository.PlaylistSubscriptionRepository;
import com.moduplaylist.core.user.entity.User;
import com.moduplaylist.core.user.exception.UserNotFoundException;
import com.moduplaylist.core.user.repository.UserRepository;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlaylistServiceImpl implements PlaylistService {

  private final PlaylistRepository playlistRepository;
  private final UserRepository userRepository;
  private final PlaylistContentRepository playlistContentRepository;
  private final PlaylistSubscriptionRepository playlistSubscriptionRepository;

  @Override
  @Transactional
  public PlaylistResponse create(UUID userId, PlaylistCreateRequest request) {
    User owner = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));

    Playlist playlist = Playlist.create(
        owner,
        request.getTitle(),
        request.getDescription()
    );

    Playlist savedPlaylist = playlistRepository.save(playlist);

    return PlaylistResponse.builder()
        .id(savedPlaylist.getId())
        .owner(UserSummary.from(owner))
        .title(savedPlaylist.getTitle())
        .description(savedPlaylist.getDescription())
        .updatedAt(savedPlaylist.getUpdatedAt())
        .subscriberCount(0L)
        .subscribedByMe(false)
        .contents(Collections.emptyList())
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

  private PlaylistResponse toResponse(Playlist playlist, UUID userId) {
    long subscriberCount = playlistSubscriptionRepository.countByPlaylist_Id(playlist.getId());

    boolean subscribedByMe = playlistSubscriptionRepository.existsByUser_IdAndPlaylist_Id(
        userId,
        playlist.getId()
    );

    List<ContentSummary> contents =
        playlistContentRepository.findAllByPlaylist_Id(playlist.getId()).stream()
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
