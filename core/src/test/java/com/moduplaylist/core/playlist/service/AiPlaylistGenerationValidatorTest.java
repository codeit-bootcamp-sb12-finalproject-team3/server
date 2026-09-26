
package com.moduplaylist.core.playlist.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.moduplaylist.core.content.entity.ContentType;
import com.moduplaylist.core.playlist.ai.AiPlaylistCandidate;
import com.moduplaylist.core.playlist.ai.AiPlaylistGenerationResult;
import com.moduplaylist.core.playlist.exception.InvalidAiPlaylistContentResultException;
import com.moduplaylist.core.playlist.exception.InvalidAiPlaylistDescriptionException;
import com.moduplaylist.core.playlist.exception.InvalidAiPlaylistTagResultException;
import com.moduplaylist.core.playlist.exception.InvalidAiPlaylistTitleException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AiPlaylistGenerationValidatorTest {

  private AiPlaylistGenerationValidator validator;
  private List<AiPlaylistCandidate> candidates;

  @BeforeEach
  void setUp() {
    validator = new AiPlaylistGenerationValidator();

    candidates = List.of(
        candidate("영화 1"),
        candidate("영화 2"),
        candidate("영화 3"),
        candidate("영화 4")
    );
  }

  @Test
  void AI_생성_결과가_null이면_예외가_발생한다() {
    assertThatThrownBy(() -> validator.validate(null, candidates))
        .isInstanceOf(InvalidAiPlaylistContentResultException.class);
  }

  @Test
  void 콘텐츠를_선택하지_않으면_예외가_발생한다() {
    AiPlaylistGenerationResult result = result(List.of());

    assertThatThrownBy(() -> validator.validate(result, candidates))
        .isInstanceOf(InvalidAiPlaylistContentResultException.class);
  }

  @Test
  void 중복된_콘텐츠를_선택하면_예외가_발생한다() {
    List<UUID> contentIds = List.of(
        candidates.get(0).getContentId(),
        candidates.get(0).getContentId(),
        candidates.get(1).getContentId(),
        candidates.get(2).getContentId()
    );

    assertThatThrownBy(() -> validator.validate(result(contentIds), candidates))
        .isInstanceOf(InvalidAiPlaylistContentResultException.class);
  }

  @Test
  void 콘텐츠가_4개_미만이면_예외가_발생한다() {
    List<UUID> contentIds = candidateIds().subList(0, 3);

    assertThatThrownBy(() -> validator.validate(result(contentIds), candidates))
        .isInstanceOf(InvalidAiPlaylistContentResultException.class);
  }

  @Test
  void 후보에_없는_콘텐츠를_선택하면_예외가_발생한다() {
    List<UUID> contentIds = List.of(
        candidates.get(0).getContentId(),
        candidates.get(1).getContentId(),
        candidates.get(2).getContentId(),
        UUID.randomUUID()
    );

    assertThatThrownBy(() -> validator.validate(result(contentIds), candidates))
        .isInstanceOf(InvalidAiPlaylistContentResultException.class);
  }

  @Test
  void 제목이_비어_있으면_예외가_발생한다() {
    AiPlaylistGenerationResult result = result(
        " ",
        "편하게 즐길 수 있는 콘텐츠입니다.",
        validTags()
    );

    assertThatThrownBy(() -> validator.validate(result, candidates))
        .isInstanceOf(InvalidAiPlaylistTitleException.class);
  }

  @Test
  void 제목이_100자를_초과하면_예외가_발생한다() {
    AiPlaylistGenerationResult result = result(
        "가".repeat(101),
        "편하게 즐길 수 있는 콘텐츠입니다.",
        validTags()
    );

    assertThatThrownBy(() -> validator.validate(result, candidates))
        .isInstanceOf(InvalidAiPlaylistTitleException.class);
  }

  @Test
  void 설명이_비어_있으면_예외가_발생한다() {
    AiPlaylistGenerationResult result = result(
        "가을밤 플레이리스트",
        " ",
        validTags()
    );

    assertThatThrownBy(() -> validator.validate(result, candidates))
        .isInstanceOf(InvalidAiPlaylistDescriptionException.class);
  }

  @Test
  void 태그가_3개_미만이면_예외가_발생한다() {
    AiPlaylistGenerationResult result = result(
        "가을밤 플레이리스트",
        "편하게 즐길 수 있는 콘텐츠입니다.",
        List.of("가을", "감성")
    );

    assertThatThrownBy(() -> validator.validate(result, candidates))
        .isInstanceOf(InvalidAiPlaylistTagResultException.class);
  }

  @Test
  void 태그가_5개를_초과하면_예외가_발생한다() {
    AiPlaylistGenerationResult result = result(
        "가을밤 플레이리스트",
        "편하게 즐길 수 있는 콘텐츠입니다.",
        List.of("가을", "감성", "힐링", "잔잔함", "주말", "여유")
    );

    assertThatThrownBy(() -> validator.validate(result, candidates))
        .isInstanceOf(InvalidAiPlaylistTagResultException.class);
  }

  @Test
  void 비어_있는_태그가_있으면_예외가_발생한다() {
    AiPlaylistGenerationResult result = result(
        "가을밤 플레이리스트",
        "편하게 즐길 수 있는 콘텐츠입니다.",
        List.of("가을", " ", "힐링")
    );

    assertThatThrownBy(() -> validator.validate(result, candidates))
        .isInstanceOf(InvalidAiPlaylistTagResultException.class);
  }

  @Test
  void 공백을_제외하고_동일한_태그가_있으면_예외가_발생한다() {
    AiPlaylistGenerationResult result = result(
        "가을밤 플레이리스트",
        "편하게 즐길 수 있는 콘텐츠입니다.",
        List.of("가을", " 가을 ", "힐링")
    );

    assertThatThrownBy(() -> validator.validate(result, candidates))
        .isInstanceOf(InvalidAiPlaylistTagResultException.class);
  }

  @Test
  void 태그가_100자를_초과하면_예외가_발생한다() {
    AiPlaylistGenerationResult result = result(
        "가을밤 플레이리스트",
        "편하게 즐길 수 있는 콘텐츠입니다.",
        List.of("가을", "감성", "가".repeat(101))
    );

    assertThatThrownBy(() -> validator.validate(result, candidates))
        .isInstanceOf(InvalidAiPlaylistTagResultException.class);
  }

  @Test
  void 정상적인_AI_생성_결과는_검증을_통과한다() {
    assertThatCode(() -> validator.validate(result(candidateIds()), candidates))
        .doesNotThrowAnyException();
  }

  private AiPlaylistGenerationResult result(List<UUID> contentIds) {
    return new AiPlaylistGenerationResult(
        "가을밤 플레이리스트",
        "편하게 즐길 수 있는 콘텐츠입니다.",
        contentIds,
        validTags()
    );
  }

  private AiPlaylistGenerationResult result(
      String title,
      String description,
      List<String> tags
  ) {
    return new AiPlaylistGenerationResult(
        title,
        description,
        candidateIds(),
        tags
    );
  }

  private List<UUID> candidateIds() {
    return candidates.stream()
        .map(AiPlaylistCandidate::getContentId)
        .toList();
  }

  private List<String> validTags() {
    return List.of("가을", "감성", "힐링");
  }

  private AiPlaylistCandidate candidate(String title) {
    return new AiPlaylistCandidate(
        UUID.randomUUID(),
        title,
        title + " 설명",
        ContentType.MOVIE,
        List.of("가을", "감성")
    );
  }
}