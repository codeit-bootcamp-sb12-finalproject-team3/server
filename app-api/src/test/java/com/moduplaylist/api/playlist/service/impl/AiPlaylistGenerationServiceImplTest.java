package com.moduplaylist.api.playlist.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moduplaylist.api.playlist.dto.AiPlaylistCreateRequest;
import com.moduplaylist.api.playlist.service.PlaylistResponseAssembler;
import com.moduplaylist.core.content.entity.ContentType;
import com.moduplaylist.core.playlist.ai.AiPlaylistCandidate;
import com.moduplaylist.core.playlist.ai.AiPlaylistCandidateProvider;
import com.moduplaylist.core.playlist.ai.AiPlaylistGenerationResult;
import com.moduplaylist.core.playlist.ai.AiPlaylistGenerator;
import com.moduplaylist.core.playlist.exception.AiPlaylistGenerationFailedException;
import com.moduplaylist.core.playlist.exception.InvalidAiPlaylistContentResultException;
import com.moduplaylist.core.playlist.exception.InvalidAiPlaylistDescriptionException;
import com.moduplaylist.core.playlist.exception.InvalidAiPlaylistTagResultException;
import com.moduplaylist.core.playlist.exception.InvalidAiPlaylistTitleException;
import com.moduplaylist.core.playlist.service.AiPlaylistPersistenceService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiPlaylistGenerationServiceImplTest {

  @Mock
  private AiPlaylistCandidateProvider candidateProvider;

  @Mock
  private AiPlaylistGenerator aiPlaylistGenerator;

  @Mock
  private AiPlaylistPersistenceService aiPlaylistPersistenceService;

  @Mock
  private PlaylistResponseAssembler playlistResponseAssembler;

  @Mock
  private AiPlaylistCreateRequest request;

  private AiPlaylistGenerationServiceImpl service;
  private List<AiPlaylistCandidate> candidates;
  private UUID userId;

  @BeforeEach
  void setUp() {
    service = new AiPlaylistGenerationServiceImpl(
        aiPlaylistGenerator,
        candidateProvider,
        playlistResponseAssembler,
        aiPlaylistPersistenceService
    );

    userId = UUID.randomUUID();

    candidates = List.of(
        candidate("힐링 영화 1"),
        candidate("힐링 영화 2"),
        candidate("감성 영화 1"),
        candidate("감성 영화 2")
    );

    when(request.getTheme()).thenReturn("주말 힐링");
    when(candidateProvider.findCandidates("주말 힐링")).thenReturn(candidates);
  }

  @Test
  void 후보에_없는_콘텐츠를_AI가_선택하면_저장하지_않는다() {
    List<UUID> contentIds = List.of(
        candidates.get(0).getContentId(),
        candidates.get(1).getContentId(),
        candidates.get(2).getContentId(),
        UUID.randomUUID()
    );

    AiPlaylistGenerationResult result = validResult(contentIds);

    when(aiPlaylistGenerator.generate("주말 힐링", candidates)).thenReturn(result);

    assertThatThrownBy(() -> service.create(userId, request))
        .isInstanceOf(InvalidAiPlaylistContentResultException.class);

    verify(aiPlaylistPersistenceService, never()).save(
        any(), any(), any(), any(), any()
    );
  }

  @Test
  void AI가_중복된_콘텐츠를_선택하면_저장하지_않는다() {
    UUID duplicatedId = candidates.get(0).getContentId();

    List<UUID> contentIds = List.of(
        duplicatedId,
        duplicatedId,
        candidates.get(1).getContentId(),
        candidates.get(2).getContentId()
    );

    AiPlaylistGenerationResult result = validResult(contentIds);

    when(aiPlaylistGenerator.generate("주말 힐링", candidates)).thenReturn(result);

    assertThatThrownBy(() -> service.create(userId, request))
        .isInstanceOf(InvalidAiPlaylistContentResultException.class);

    verify(aiPlaylistPersistenceService, never()).save(
        any(), any(), any(), any(), any()
    );
  }

  @Test
  void AI가_콘텐츠를_4개_미만으로_선택하면_저장하지_않는다() {
    List<UUID> contentIds = List.of(
        candidates.get(0).getContentId(),
        candidates.get(1).getContentId(),
        candidates.get(2).getContentId()
    );

    AiPlaylistGenerationResult result = validResult(contentIds);

    when(aiPlaylistGenerator.generate("주말 힐링", candidates)).thenReturn(result);

    assertThatThrownBy(() -> service.create(userId, request))
        .isInstanceOf(InvalidAiPlaylistContentResultException.class);

    verify(aiPlaylistPersistenceService, never()).save(
        any(), any(), any(), any(), any()
    );
  }

  @Test
  void AI가_잘못된_태그를_반환하면_저장하지_않는다() {
    AiPlaylistGenerationResult result = new AiPlaylistGenerationResult(
        "주말 힐링 플레이리스트",
        "편하게 보기 좋은 콘텐츠입니다.",
        candidateIds(),
        List.of("힐링", "힐링", "감성")
    );

    when(aiPlaylistGenerator.generate("주말 힐링", candidates)).thenReturn(result);

    assertThatThrownBy(() -> service.create(userId, request))
        .isInstanceOf(InvalidAiPlaylistTagResultException.class);

    verify(aiPlaylistPersistenceService, never()).save(
        any(), any(), any(), any(), any()
    );
  }

  @Test
  void AI가_빈_제목을_반환하면_저장하지_않는다() {
    AiPlaylistGenerationResult result = new AiPlaylistGenerationResult(
        " ",
        "편하게 보기 좋은 콘텐츠입니다.",
        candidateIds(),
        List.of("힐링", "감성", "잔잔함")
    );

    when(aiPlaylistGenerator.generate("주말 힐링", candidates)).thenReturn(result);

    assertThatThrownBy(() -> service.create(userId, request))
        .isInstanceOf(InvalidAiPlaylistTitleException.class);

    verify(aiPlaylistPersistenceService, never()).save(
        any(), any(), any(), any(), any()
    );
  }

  @Test
  void AI가_빈_설명을_반환하면_저장하지_않는다() {
    AiPlaylistGenerationResult result = new AiPlaylistGenerationResult(
        "주말 힐링 플레이리스트",
        " ",
        candidateIds(),
        List.of("힐링", "감성", "잔잔함")
    );

    when(aiPlaylistGenerator.generate("주말 힐링", candidates)).thenReturn(result);

    assertThatThrownBy(() -> service.create(userId, request))
        .isInstanceOf(InvalidAiPlaylistDescriptionException.class);

    verify(aiPlaylistPersistenceService, never()).save(
        any(), any(), any(), any(), any()
    );
  }

  private AiPlaylistGenerationResult validResult(List<UUID> contentIds) {
    return new AiPlaylistGenerationResult(
        "주말 힐링 플레이리스트",
        "편하게 보기 좋은 콘텐츠입니다.",
        contentIds,
        List.of("힐링", "감성", "잔잔함")
    );
  }

  private List<UUID> candidateIds() {
    return candidates.stream()
        .map(AiPlaylistCandidate::getContentId)
        .toList();
  }

  private AiPlaylistCandidate candidate(String title) {
    return new AiPlaylistCandidate(
        UUID.randomUUID(),
        title,
        title + " 설명",
        ContentType.MOVIE,
        List.of("힐링", "감성")
    );
  }

  @Test
  void 정상_AI_결과는_저장_서비스로_전달한다() {
    AiPlaylistGenerationResult result = new AiPlaylistGenerationResult(
        "주말 힐링 플레이리스트",
        "편하게 보기 좋은 콘텐츠입니다.",
        candidateIds(),
        List.of("힐링", "감성", "잔잔함")
    );

    when(aiPlaylistGenerator.generate("주말 힐링", candidates)).thenReturn(result);

    service.create(userId, request);

    verify(aiPlaylistPersistenceService).save(
        eq(userId),
        eq("주말 힐링 플레이리스트"),
        eq("편하게 보기 좋은 콘텐츠입니다."),
        eq(candidateIds()),
        eq(List.of("힐링", "감성", "잔잔함"))
    );
  }

  @Test
  void AI_호출이_실패하면_저장하지_않는다() {
    when(aiPlaylistGenerator.generate("주말 힐링", candidates))
        .thenThrow(new AiPlaylistGenerationFailedException());

    assertThatThrownBy(() -> service.create(userId, request))
        .isInstanceOf(AiPlaylistGenerationFailedException.class);

    verify(aiPlaylistPersistenceService, never()).save(
        any(), any(), any(), any(), any()
    );
  }
}