package com.moduplaylist.batch.job.aiplaylist;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;

import com.moduplaylist.core.playlist.ai.AiPlaylistCandidate;
import com.moduplaylist.core.playlist.ai.AiPlaylistCandidateProvider;
import com.moduplaylist.core.playlist.ai.AiPlaylistGenerationResult;
import com.moduplaylist.core.playlist.ai.AiPlaylistGenerator;
import com.moduplaylist.core.playlist.ai.AiPlaylistThemeGenerator;
import com.moduplaylist.core.playlist.exception.AiPlaylistOwnerNotFoundException;
import com.moduplaylist.core.playlist.exception.InvalidAiPlaylistContentResultException;
import com.moduplaylist.core.playlist.exception.InvalidAiPlaylistTitleException;
import com.moduplaylist.core.playlist.repository.PlaylistRepository;
import com.moduplaylist.core.playlist.service.AiPlaylistGenerationValidator;
import com.moduplaylist.core.playlist.service.AiPlaylistPersistenceService;
import com.moduplaylist.core.user.entity.User;
import com.moduplaylist.core.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AiPlaylistAutoGenerationServiceTest {

  private static final String OWNER_EMAIL = "ai@mopl.com";

  @Mock
  private UserRepository userRepository;

  @Mock
  private PlaylistRepository playlistRepository;

  @Mock
  private AiPlaylistThemeGenerator themeGenerator;

  @Mock
  private AiPlaylistCandidateProvider candidateProvider;

  @Mock
  private AiPlaylistGenerator aiPlaylistGenerator;

  @Mock
  private AiPlaylistGenerationValidator aiPlaylistGenerationValidator;

  @Mock
  private AiPlaylistPersistenceService aiPlaylistPersistenceService;

  @Mock
  private User aiOwner;

  private AiPlaylistAutoGenerationService service;
  private UUID ownerId;
  private LocalDate date;

  @BeforeEach
  void setUp() {
    service = new AiPlaylistAutoGenerationService(
        userRepository,
        playlistRepository,
        themeGenerator,
        candidateProvider,
        aiPlaylistGenerator,
        aiPlaylistGenerationValidator,
        aiPlaylistPersistenceService
    );

    ReflectionTestUtils.setField(service, "ownerEmail", OWNER_EMAIL);

    ownerId = UUID.randomUUID();
    date = LocalDate.of(2026, 9, 25);
  }

  @Test
  void AI_전용_계정이_없으면_예외가_발생한다() {
    when(userRepository.findByEmail(OWNER_EMAIL)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.generate(date))
        .isInstanceOf(AiPlaylistOwnerNotFoundException.class);

    verify(themeGenerator, never()).generate(any(), any());
    verify(aiPlaylistPersistenceService, never()).save(
        any(), any(), any(), any(), any()
    );
  }

  @Test
  void 콘텐츠_후보가_4개_미만이면_예외가_발생하고_저장하지_않는다() {
    givenOwner();

    List<String> existingTitles = List.of("기존 플레이리스트");
    List<AiPlaylistCandidate> candidates = List.of(
        candidate(),
        candidate(),
        candidate()
    );

    when(playlistRepository.findRecentTitlesByOwnerId(eq(ownerId), any()))
        .thenReturn(existingTitles);
    when(themeGenerator.generate(date, existingTitles)).thenReturn("가을 밤");
    when(candidateProvider.findCandidates("가을 밤")).thenReturn(candidates);

    assertThatThrownBy(() -> service.generate(date))
        .isInstanceOf(InvalidAiPlaylistContentResultException.class);

    verify(aiPlaylistGenerator, never()).generate(any(), any());
    verify(aiPlaylistPersistenceService, never()).save(
        any(), any(), any(), any(), any()
    );
  }

  @Test
  void 생성된_제목이_이미_존재하면_저장하지_않는다() {
    givenOwner();

    List<String> existingTitles = List.of("기존 플레이리스트");
    List<AiPlaylistCandidate> candidates = candidates();
    AiPlaylistGenerationResult result = result();

    when(playlistRepository.findRecentTitlesByOwnerId(eq(ownerId), any()))
        .thenReturn(existingTitles);
    when(themeGenerator.generate(date, existingTitles)).thenReturn("가을 밤");
    when(candidateProvider.findCandidates("가을 밤")).thenReturn(candidates);
    when(aiPlaylistGenerator.generate("가을 밤", candidates)).thenReturn(result);
    when(playlistRepository.existsByOwner_IdAndTitle(ownerId, result.getTitle()))
        .thenReturn(true);

    assertThatThrownBy(() -> service.generate(date))
        .isInstanceOf(InvalidAiPlaylistTitleException.class);

    verify(aiPlaylistGenerationValidator).validate(result, candidates);
    verify(aiPlaylistPersistenceService, never()).save(
        any(), any(), any(), any(), any()
    );
  }

  @Test
  void 정상_생성_결과는_AI_전용_계정의_플레이리스트로_저장한다() {
    givenOwner();

    List<String> existingTitles = List.of(
        "주말 힐링",
        "비 오는 날의 영화"
    );
    List<AiPlaylistCandidate> candidates = candidates();
    AiPlaylistGenerationResult result = result();

    when(playlistRepository.findRecentTitlesByOwnerId(eq(ownerId), any()))
        .thenReturn(existingTitles);
    when(themeGenerator.generate(date, existingTitles)).thenReturn("가을 밤");
    when(candidateProvider.findCandidates("가을 밤")).thenReturn(candidates);
    when(aiPlaylistGenerator.generate("가을 밤", candidates)).thenReturn(result);
    when(playlistRepository.existsByOwner_IdAndTitle(ownerId, result.getTitle()))
        .thenReturn(false);

    service.generate(date);

    verify(themeGenerator).generate(date, existingTitles);
    verify(candidateProvider).findCandidates("가을 밤");
    verify(aiPlaylistGenerator).generate("가을 밤", candidates);
    verify(aiPlaylistGenerationValidator).validate(result, candidates);
    verify(aiPlaylistPersistenceService).save(
        ownerId,
        result.getTitle(),
        result.getDescription(),
        result.getContentIds(),
        result.getTags()
    );
  }

  @Test
  void 이번_주_생성분이_없으면_5개를_생성한다() {
    givenOwner();
    givenSuccessfulGeneration();

    when(playlistRepository.countCreatedByOwnerInWeek(ownerId, weekStart(), nextWeekStart()))
        .thenReturn(0L);

    service.generateWeekly(date);

    verify(aiPlaylistPersistenceService, times(5)).save(
        any(), any(), any(), any(), any()
    );
  }

  @Test
  void 이번_주에_3개가_있으면_2개만_생성한다() {
    givenOwner();
    givenSuccessfulGeneration();

    when(playlistRepository.countCreatedByOwnerInWeek(ownerId, weekStart(), nextWeekStart()))
        .thenReturn(3L);

    service.generateWeekly(date);

    verify(aiPlaylistPersistenceService, times(2)).save(
        any(), any(), any(), any(), any()
    );
  }

  @Test
  void 이번_주에_이미_5개가_있으면_추가_생성하지_않는다() {
    givenOwner();

    when(playlistRepository.countCreatedByOwnerInWeek(ownerId, weekStart(), nextWeekStart()))
        .thenReturn(5L);

    service.generateWeekly(date);

    verify(themeGenerator, never()).generate(any(), any());
    verify(aiPlaylistPersistenceService, never()).save(
        any(), any(), any(), any(), any()
    );
  }

  @Test
  void 일부_생성에_실패하면_재실행_시_부족한_개수만_생성한다() {
    givenOwner();

    List<String> existingTitles = List.of("기존 플레이리스트");
    List<AiPlaylistCandidate> candidates = candidates();
    AiPlaylistGenerationResult result = result();
    AtomicInteger generationAttempts = new AtomicInteger();

    when(playlistRepository.countCreatedByOwnerInWeek(ownerId, weekStart(), nextWeekStart()))
        .thenReturn(0L, 3L);

    when(playlistRepository.findRecentTitlesByOwnerId(eq(ownerId), any()))
        .thenReturn(existingTitles);

    when(themeGenerator.generate(date, existingTitles)).thenAnswer(invocation -> {
      if (generationAttempts.incrementAndGet() == 4) {
        throw new IllegalStateException("AI 생성 실패");
      }
      return "가을 밤";
    });

    when(candidateProvider.findCandidates("가을 밤")).thenReturn(candidates);
    when(aiPlaylistGenerator.generate("가을 밤", candidates)).thenReturn(result);

    assertThatThrownBy(() -> service.generateWeekly(date))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("AI 생성 실패");

    verify(aiPlaylistPersistenceService, times(3)).save(
        any(), any(), any(), any(), any()
    );

    service.generateWeekly(date);

    verify(aiPlaylistPersistenceService, times(5)).save(
        any(), any(), any(), any(), any()
    );

    assertThat(generationAttempts.get()).isEqualTo(6);
  }

  private void givenOwner() {
    when(userRepository.findByEmail(OWNER_EMAIL)).thenReturn(Optional.of(aiOwner));
    when(aiOwner.getId()).thenReturn(ownerId);
  }

  private List<AiPlaylistCandidate> candidates() {
    return List.of(
        candidate(),
        candidate(),
        candidate(),
        candidate()
    );
  }

  private AiPlaylistCandidate candidate() {
    return org.mockito.Mockito.mock(AiPlaylistCandidate.class);
  }

  private AiPlaylistGenerationResult result() {
    return new AiPlaylistGenerationResult(
        "선선한 가을밤 플레이리스트",
        "가을밤에 편하게 즐길 수 있는 콘텐츠입니다.",
        List.of(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID()
        ),
        List.of("가을", "감성", "힐링")
    );
  }

  private void givenSuccessfulGeneration() {
    List<String> existingTitles = List.of("기존 플레이리스트");
    List<AiPlaylistCandidate> candidates = candidates();
    AiPlaylistGenerationResult result = result();

    when(playlistRepository.findRecentTitlesByOwnerId(eq(ownerId), any()))
        .thenReturn(existingTitles);
    when(themeGenerator.generate(date, existingTitles)).thenReturn("가을 밤");
    when(candidateProvider.findCandidates("가을 밤")).thenReturn(candidates);
    when(aiPlaylistGenerator.generate("가을 밤", candidates)).thenReturn(result);
  }

  private Instant weekStart() {
    return LocalDate.of(2026, 9, 21)
        .atStartOfDay(ZoneId.of("Asia/Seoul"))
        .toInstant();
  }

  private Instant nextWeekStart() {
    return LocalDate.of(2026, 9, 28)
        .atStartOfDay(ZoneId.of("Asia/Seoul"))
        .toInstant();
  }
}