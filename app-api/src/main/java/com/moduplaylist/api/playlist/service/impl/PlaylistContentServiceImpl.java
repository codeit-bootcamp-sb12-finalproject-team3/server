package com.moduplaylist.api.playlist.service.impl;

import com.github.f4b6a3.uuid.UuidCreator;
import com.moduplaylist.api.playlist.event.PlaylistContentActivityEvent;
import com.moduplaylist.api.playlist.event.PlaylistContentAddedEvent;
import com.moduplaylist.api.playlist.event.PlaylistTagRecalculationEvent;
import com.moduplaylist.api.playlist.service.PlaylistContentService;
import com.moduplaylist.core.activity.enums.ContentActivityType;
import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.exception.ContentNotFoundException;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.core.playlist.entity.Playlist;
import com.moduplaylist.core.playlist.entity.PlaylistContent;
import com.moduplaylist.core.playlist.exception.PlaylistAccessDeniedException;
import com.moduplaylist.core.playlist.exception.PlaylistContentAlreadyExistsException;
import com.moduplaylist.core.playlist.exception.PlaylistContentNotFoundException;
import com.moduplaylist.core.playlist.exception.PlaylistNotFoundException;
import com.moduplaylist.core.playlist.policy.PlaylistContentPolicy;
import com.moduplaylist.core.playlist.repository.PlaylistContentRepository;
import com.moduplaylist.core.playlist.repository.PlaylistRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlaylistContentServiceImpl implements PlaylistContentService {

  private final PlaylistRepository playlistRepository;
  private final ContentRepository contentRepository;
  private final PlaylistContentRepository playlistContentRepository;
  private final ApplicationEventPublisher eventPublisher;

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

    PlaylistContentPolicy.validateContent(content);

    if (playlistContentRepository.existsByPlaylist_IdAndContent_Id(
        playlistId,
        contentId
    )) {
      throw new PlaylistContentAlreadyExistsException(
          playlistId,
          contentId
      );
    }

    PlaylistContent playlistContent = PlaylistContent.create(
        playlist,
        content
    );

    try {
      playlistContentRepository.saveAndFlush(playlistContent);
    } catch (DataIntegrityViolationException e) {
      throw new PlaylistContentAlreadyExistsException(
          playlistId,
          contentId,
          e
      );
    }

    eventPublisher.publishEvent(
        new PlaylistTagRecalculationEvent(playlist.getId())
    );

    eventPublisher.publishEvent(
        new PlaylistContentAddedEvent(
            playlist.getOwner().getId(),
            playlistId,
            contentId,
            playlist.getTitle(),
            content.getTitle(),
            playlistContent.getCreatedAt()
        )
    );

    eventPublisher.publishEvent(
        new PlaylistContentActivityEvent(
            playlistContent.getId(),
            ContentActivityType.PLAYLIST_CONTENT_ADDED,
            userId,
            contentId,
            playlistContent.getCreatedAt()
        )
    );
  }

  @Override
  @Transactional
  public void removeContent(UUID userId, UUID playlistId, UUID contentId) {
    Playlist playlist = playlistRepository.findByIdForUpdate(playlistId)
        .orElseThrow(() -> new PlaylistNotFoundException(playlistId));

    if (!playlist.getOwner().getId().equals(userId)) {
      throw new PlaylistAccessDeniedException(playlistId);
    }

    Content content = contentRepository.findById(contentId)
        .orElseThrow(() -> new ContentNotFoundException(contentId));

    PlaylistContent playlistContent = playlistContentRepository
        .findByPlaylist_IdAndContent_Id(
            playlistId,
            contentId
        )
        .orElseThrow(
            () -> new PlaylistContentNotFoundException(
                playlistId,
                contentId
            )
        );

    long visibleContentCount =
        playlistContentRepository
            .countByPlaylist_IdAndContent_HiddenFalse(
                playlistId
            );

    PlaylistContentPolicy.validateRemoval(
        content,
        visibleContentCount
    );

    playlistContentRepository.delete(playlistContent);

    eventPublisher.publishEvent(
        new PlaylistTagRecalculationEvent(playlist.getId())
    );

    eventPublisher.publishEvent(
        new PlaylistContentActivityEvent(
            UuidCreator.getTimeOrderedEpoch(),
            ContentActivityType.PLAYLIST_CONTENT_REMOVED,
            userId,
            contentId,
            Instant.now()
        )
    );
  }
}
