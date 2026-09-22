package com.moduplaylist.api.playlist.service;

import com.moduplaylist.api.content.dto.ContentSummary;
import com.moduplaylist.api.playlist.dto.PlaylistResponse;
import com.moduplaylist.api.user.dto.UserSummary;
import com.moduplaylist.core.playlist.entity.Playlist;
import com.moduplaylist.core.playlist.entity.PlaylistContent;
import com.moduplaylist.core.playlist.repository.PlaylistContentRepository;
import com.moduplaylist.core.playlist.repository.PlaylistSubscriptionRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlaylistResponseAssembler {

  private final PlaylistSubscriptionRepository playlistSubscriptionRepository;
  private final PlaylistContentRepository playlistContentRepository;

  public PlaylistResponse toResponse(
      Playlist playlist,
      UUID userId
  ) {
    long subscriberCount =
        playlistSubscriptionRepository.countByPlaylist_Id(
            playlist.getId()
        );

    boolean subscribedByMe =
        playlistSubscriptionRepository.existsByUser_IdAndPlaylist_Id(
            userId,
            playlist.getId()
        );

    List<ContentSummary> contents =
        playlistContentRepository
            .findAllByPlaylist_IdAndContent_HiddenFalseOrderByCreatedAtAscIdAsc(
                playlist.getId()
            )
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
