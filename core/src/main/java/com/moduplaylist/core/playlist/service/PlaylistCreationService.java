package com.moduplaylist.core.playlist.service;

import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.exception.ContentNotFoundException;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.core.playlist.entity.Playlist;
import com.moduplaylist.core.playlist.entity.PlaylistContent;
import com.moduplaylist.core.playlist.exception.InvalidPlaylistContentRequestException;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlaylistCreationService {

  private final PlaylistRepository playlistRepository;
  private final UserRepository userRepository;
  private final ContentRepository contentRepository;
  private final PlaylistContentRepository playlistContentRepository;

  @Transactional
  public Playlist create(
      UUID ownerId,
      String title,
      String description,
      List<UUID> contentIds
  ) {
    User owner = userRepository.findById(ownerId)
        .orElseThrow(() -> new UserNotFoundException(ownerId));

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
        title,
        description
    );

    Playlist savedPlaylist = playlistRepository.save(playlist);

    List<PlaylistContent> playlistContents = contents.stream()
        .map(content -> PlaylistContent.create(savedPlaylist, content))
        .toList();

    playlistContentRepository.saveAll(playlistContents);

    return savedPlaylist;
  }
}
