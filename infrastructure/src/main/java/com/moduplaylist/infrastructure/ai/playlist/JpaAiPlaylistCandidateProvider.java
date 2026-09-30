package com.moduplaylist.infrastructure.ai.playlist;

import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.entity.ContentType;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.core.content.repository.ContentTagRepository;
import com.moduplaylist.core.playlist.ai.AiPlaylistCandidate;
import com.moduplaylist.core.playlist.ai.AiPlaylistCandidateProvider;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class JpaAiPlaylistCandidateProvider implements AiPlaylistCandidateProvider {

  private static final int CANDIDATE_LIMIT = 100;

  private static final List<ContentType> CANDIDATE_TYPES = List.of(
      ContentType.MOVIE,
      ContentType.TV_SEASON
  );

  private final ContentRepository contentRepository;
  private final ContentTagRepository contentTagRepository;

  @Override
  @Transactional(readOnly = true)
  public List<AiPlaylistCandidate> findCandidates(String theme) {
    if (CANDIDATE_TYPES.isEmpty()) {
      return List.of();
    }

    List<Content> contents = contentRepository.findVisibleByTypeIn(
        CANDIDATE_TYPES,
        PageRequest.of(0, CANDIDATE_LIMIT)
    );

    if (contents.isEmpty()) {
      return List.of();
    }

    List<UUID> contentIds = contents.stream()
        .map(Content::getId)
        .toList();

    Map<UUID, List<String>> tagByContentId =
        contentTagRepository.findAllWithTagByContentIdIn(contentIds)
            .stream()
            .collect(Collectors.groupingBy(
                contentTag -> contentTag.getContent().getId(),
                Collectors.mapping(
                    contentTag -> contentTag.getTag().getName(),
                    Collectors.toList()
                )
            ));

    return contents.stream()
        .map(content -> new AiPlaylistCandidate(
            content.getId(),
            content.getTitle(),
            content.getDescription(),
            content.getType(),
            tagByContentId.getOrDefault(content.getId(), List.of())
        ))
        .toList();
  }
}
