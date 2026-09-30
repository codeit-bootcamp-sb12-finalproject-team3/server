package com.moduplaylist.core.playlist.service;

import com.github.f4b6a3.uuid.UuidCreator;
import com.moduplaylist.core.content.entity.Tag;
import com.moduplaylist.core.content.repository.TagRepository;
import com.moduplaylist.core.playlist.entity.Playlist;
import com.moduplaylist.core.playlist.entity.PlaylistTag;
import com.moduplaylist.core.playlist.exception.PlaylistNotFoundException;
import com.moduplaylist.core.playlist.repository.PlaylistRepository;
import com.moduplaylist.core.playlist.repository.PlaylistTagRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlaylistTagAssignmentService {

  private final PlaylistRepository playlistRepository;
  private final PlaylistTagRepository playlistTagRepository;
  private final TagRepository tagRepository;

  @Transactional
  public void assign(UUID playlistId, List<String> tagNames) {
    Playlist playlist = playlistRepository.findById(playlistId)
        .orElseThrow(() -> new PlaylistNotFoundException(playlistId));

    List<String> normalizedTagNames = tagNames.stream()
        .map(String::strip)
        .distinct()
        .toList();

    Set<String> existingTagNames = tagRepository.findAllByNameIn(normalizedTagNames).stream()
        .map(Tag::getName)
        .collect(Collectors.toSet());

    normalizedTagNames.stream()
        .filter(name -> !existingTagNames.contains(name))
        .forEach(name -> tagRepository.upsert(UuidCreator.getTimeOrderedEpoch(), name));

    List<Tag> tags = tagRepository.findAllByNameIn(normalizedTagNames);

    playlistTagRepository.deleteAllByPlaylist_Id(playlistId);
    playlistTagRepository.flush();

    List<PlaylistTag> playlistTags = tags.stream()
        .map(tag -> PlaylistTag.create(playlist, tag))
        .toList();

    playlistTagRepository.saveAll(playlistTags);
  }
}
