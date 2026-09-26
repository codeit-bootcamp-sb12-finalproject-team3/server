package com.moduplaylist.core.playlist.service;

import com.moduplaylist.core.playlist.ai.AiPlaylistCandidate;
import com.moduplaylist.core.playlist.ai.AiPlaylistGenerationResult;
import com.moduplaylist.core.playlist.exception.InvalidAiPlaylistContentResultException;
import com.moduplaylist.core.playlist.exception.InvalidAiPlaylistDescriptionException;
import com.moduplaylist.core.playlist.exception.InvalidAiPlaylistTagResultException;
import com.moduplaylist.core.playlist.exception.InvalidAiPlaylistTitleException;
import com.moduplaylist.core.playlist.policy.PlaylistContentPolicy;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class AiPlaylistGenerationValidator {

  public void validate(
      AiPlaylistGenerationResult result,
      List<AiPlaylistCandidate> candidates
  ) {
    if (result == null) {
      throw new InvalidAiPlaylistContentResultException("AI 생성 결과가 존재하지 않습니다.");
    }

    List<UUID> contentIds = result.getContentIds();

    if (contentIds == null || contentIds.isEmpty()) {
      throw new InvalidAiPlaylistContentResultException("AI가 콘텐츠를 선택하지 않았습니다.");
    }

    Set<UUID> uniqueContentIds = new HashSet<>(contentIds);

    if (uniqueContentIds.size() != contentIds.size()) {
      throw new InvalidAiPlaylistContentResultException(
          "AI 생성 결과에 중복된 콘텐츠가 포함되어 있습니다."
      );
    }

    if (uniqueContentIds.size() < PlaylistContentPolicy.MIN_CONTENT_COUNT) {
      throw new InvalidAiPlaylistContentResultException(
          "AI가 최소 콘텐츠 수보다 적은 콘텐츠를 선택했습니다."
      );
    }

    Set<UUID> candidateIds = candidates.stream()
        .map(AiPlaylistCandidate::getContentId)
        .collect(Collectors.toSet());

    if (!candidateIds.containsAll(uniqueContentIds)) {
      throw new InvalidAiPlaylistContentResultException(
          "AI가 서버에서 제공하지 않은 콘텐츠를 선택했습니다."
      );
    }

    if (result.getTitle() == null || result.getTitle().isBlank()) {
      throw new InvalidAiPlaylistTitleException(
          "AI가 생성한 플레이리스트 제목이 비어 있습니다."
      );
    }

    if (result.getTitle().length() > 100) {
      throw new InvalidAiPlaylistTitleException(
          "AI가 생성한 플레이리스트 제목은 100자 이하여야 합니다."
      );
    }

    if (result.getDescription() == null || result.getDescription().isBlank()) {
      throw new InvalidAiPlaylistDescriptionException(
          "AI가 생성한 플레이리스트 설명이 비어 있습니다."
      );
    }

    List<String> tags = result.getTags();

    if (tags == null || tags.size() < 3 || tags.size() > 5) {
      throw new InvalidAiPlaylistTagResultException(
          "AI가 생성한 태그는 3개 이상 5개 이하여야 합니다."
      );
    }

    if (tags.stream().anyMatch(tag -> tag == null || tag.isBlank())) {
      throw new InvalidAiPlaylistTagResultException(
          "AI 생성 결과에 비어 있는 태그가 포함되어 있습니다."
      );
    }

    Set<String> normalizedTags = tags.stream()
        .map(String::strip)
        .collect(Collectors.toSet());

    if (normalizedTags.size() != tags.size()) {
      throw new InvalidAiPlaylistTagResultException(
          "AI 생성 결과에 중복된 태그가 포함되어 있습니다."
      );
    }

    if (normalizedTags.stream().anyMatch(tag -> tag.length() > 100)) {
      throw new InvalidAiPlaylistTagResultException(
          "AI가 생성한 태그는 100자 이하여야 합니다."
      );
    }
  }

}
