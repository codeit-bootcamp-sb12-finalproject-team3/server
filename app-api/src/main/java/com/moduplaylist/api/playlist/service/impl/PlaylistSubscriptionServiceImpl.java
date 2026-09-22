package com.moduplaylist.api.playlist.service.impl;

import com.moduplaylist.api.playlist.event.PlaylistSubscribedEvent;
import com.moduplaylist.api.playlist.service.PlaylistSubscriptionService;
import com.moduplaylist.api.recommendation.service.PlaylistPreferenceUpdateService;
import com.moduplaylist.core.activity.enums.PlaylistActivityType;
import com.moduplaylist.core.playlist.entity.Playlist;
import com.moduplaylist.core.playlist.entity.PlaylistSubscription;
import com.moduplaylist.core.playlist.exception.PlaylistAlreadySubscribedException;
import com.moduplaylist.core.playlist.exception.PlaylistNotFoundException;
import com.moduplaylist.core.playlist.exception.PlaylistSubscriptionNotFoundException;
import com.moduplaylist.core.playlist.exception.SelfPlaylistSubscriptionNotAllowedException;
import com.moduplaylist.core.playlist.repository.PlaylistRepository;
import com.moduplaylist.core.playlist.repository.PlaylistSubscriptionRepository;
import com.moduplaylist.core.user.entity.User;
import com.moduplaylist.core.user.exception.UserNotFoundException;
import com.moduplaylist.core.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlaylistSubscriptionServiceImpl implements PlaylistSubscriptionService {

  private final PlaylistRepository playlistRepository;
  private final PlaylistSubscriptionRepository playlistSubscriptionRepository;
  private final UserRepository userRepository;
  private final PlaylistPreferenceUpdateService playlistPreferenceUpdateService;
  private final ApplicationEventPublisher eventPublisher;

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

    playlistPreferenceUpdateService.applyActivity(
        userId,
        playlistId,
        PlaylistActivityType.PLAYLIST_SUBSCRIBED
    );

    eventPublisher.publishEvent(
        new PlaylistSubscribedEvent(
            userId,
            playlist.getOwner().getId(),
            playlistId
        )
    );
  }

  @Override
  @Transactional
  public void unsubscribe(UUID userId, UUID playlistId) {
    playlistRepository.findById(playlistId)
        .orElseThrow(() -> new PlaylistNotFoundException(playlistId));

    PlaylistSubscription subscription =
        playlistSubscriptionRepository
            .findByUser_IdAndPlaylist_Id(userId, playlistId)
            .orElseThrow(
                () -> new PlaylistSubscriptionNotFoundException(userId, playlistId)
            );

    playlistPreferenceUpdateService.applyActivity(
        userId,
        playlistId,
        PlaylistActivityType.PLAYLIST_UNSUBSCRIBED
    );

    playlistSubscriptionRepository.delete(subscription);
  }
}
