package com.moduplaylist.batch.job.aiplaylist;

import com.moduplaylist.core.playlist.ai.AiPlaylistCandidate;
import com.moduplaylist.core.playlist.ai.AiPlaylistCandidateProvider;
import com.moduplaylist.core.playlist.ai.AiPlaylistGenerationResult;
import com.moduplaylist.core.playlist.ai.AiPlaylistGenerator;
import com.moduplaylist.core.playlist.ai.AiPlaylistThemeGenerator;
import com.moduplaylist.core.playlist.exception.AiPlaylistOwnerNotFoundException;
import com.moduplaylist.core.playlist.exception.InvalidAiPlaylistContentResultException;
import com.moduplaylist.core.playlist.exception.InvalidAiPlaylistTitleException;
import com.moduplaylist.core.playlist.exception.AiPlaylistGenerationFailedException;
import com.moduplaylist.core.playlist.policy.PlaylistContentPolicy;
import com.moduplaylist.core.playlist.repository.PlaylistRepository;
import com.moduplaylist.core.playlist.service.AiPlaylistGenerationValidator;
import com.moduplaylist.core.playlist.service.AiPlaylistPersistenceService;
import com.moduplaylist.core.user.repository.UserRepository;
import com.moduplaylist.core.playlist.ai.AiPlaylistSeasonalContext;
import com.moduplaylist.core.playlist.ai.AiPlaylistSeasonalContextProvider;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiPlaylistAutoGenerationService {

  private static final int RECENT_PLAYLIST_TITLE_LIMIT = 30;
  private static final int WEEKLY_PLAYLIST_COUNT = 5;
  private static final int MAX_GENERATION_ATTEMPTS = 3;
  private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");

  private final UserRepository userRepository;
  private final PlaylistRepository playlistRepository;
  private final AiPlaylistThemeGenerator themeGenerator;
  private final AiPlaylistSeasonalContextProvider seasonalContextProvider;
  private final AiPlaylistCandidateProvider candidateProvider;
  private final AiPlaylistGenerator aiPlaylistGenerator;
  private final AiPlaylistGenerationValidator aiPlaylistGenerationValidator;
  private final AiPlaylistPersistenceService aiPlaylistPersistenceService;

  @Value("${mopl.batch.ai-playlist.owner-email}")
  private String ownerEmail;

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void generateWeekly(LocalDate date) {
    UUID ownerId = userRepository.findByEmail(ownerEmail)
        .orElseThrow(() -> new AiPlaylistOwnerNotFoundException(ownerEmail))
        .getId();

    LocalDate weekStartDate = date.with(
        TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
    );

    Instant weekStart = weekStartDate.atStartOfDay(ZONE_ID).toInstant();
    Instant nextWeekStart = weekStartDate.plusWeeks(1).atStartOfDay(ZONE_ID).toInstant();

    long createdCount = playlistRepository.countCreatedByOwnerInWeek(
        ownerId,
        weekStart,
        nextWeekStart
    );

    long remainingCount = Math.max(0, WEEKLY_PLAYLIST_COUNT - createdCount);

    log.info(
        "AI 플레이리스트 주간 생성 시작 - date={}, existingCount={}, remainingCount={}",
        date,
        createdCount,
        remainingCount
    );

    if (remainingCount == 0) {
      log.info("AI 플레이리스트 주간 생성 생략 - 이미 {}개 존재", createdCount);
      return;
    }

    AiPlaylistSeasonalContext seasonalContext = seasonalContextProvider.getContext(date);

    int generatedCount = 0;
    int failedCount = 0;
    RuntimeException lastFailure = null;

    for (int i = 0; i < remainingCount; i++) {
      boolean generated = false;

      for (int attempt = 1; attempt <= MAX_GENERATION_ATTEMPTS; attempt++) {
        try {
          generate(date, seasonalContext);

          generatedCount++;
          generated = true;

          log.info(
              "AI 플레이리스트 생성 진행 - {}/{}개 완료",
              generatedCount,
              remainingCount
          );

          break;
        } catch (InvalidAiPlaylistContentResultException
                 | InvalidAiPlaylistTitleException
                 | AiPlaylistGenerationFailedException exception) {
          lastFailure = exception;

          log.warn(
              "AI 플레이리스트 생성 실패 - slot={}, attempt={}/{}, reason={}",
              i + 1,
              attempt,
              MAX_GENERATION_ATTEMPTS,
              exception.getMessage()
          );
        }
      }

      if (!generated) {
        failedCount++;
        log.error("AI 플레이리스트 생성 최종 실패 - slot={}", i + 1);
      }
    }

    log.info(
        "AI 플레이리스트 주간 생성 종료 - date={}, existingCount={}, generatedCount={}, failedCount={}",
        date,
        createdCount,
        generatedCount,
        failedCount
    );

    if (failedCount > 0) {
      throw new IllegalStateException(
          "AI 플레이리스트 주간 생성 실패 - 부족한 플레이리스트 " + failedCount + "개",
          lastFailure
      );
    }
  }

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void generate(LocalDate date) {
    AiPlaylistSeasonalContext seasonalContext = seasonalContextProvider.getContext(date);
    generate(date, seasonalContext);
  }

  private void generate(LocalDate date, AiPlaylistSeasonalContext seasonalContext) {
    UUID ownerId = userRepository.findByEmail(ownerEmail)
        .orElseThrow(() -> new AiPlaylistOwnerNotFoundException(ownerEmail))
        .getId();

    List<String> existingPlaylistTitles = playlistRepository.findRecentTitlesByOwnerId(
        ownerId,
        PageRequest.of(0, RECENT_PLAYLIST_TITLE_LIMIT)
    );

    String theme = themeGenerator.generate(date, existingPlaylistTitles, seasonalContext);

    List<AiPlaylistCandidate> candidates = candidateProvider.findCandidates(theme);

    if (candidates.size() < PlaylistContentPolicy.MIN_CONTENT_COUNT) {
      throw new InvalidAiPlaylistContentResultException(
          "플레이리스트를 생성할 수 있는 콘텐츠 후보가 부족합니다."
      );
    }

    AiPlaylistGenerationResult result = aiPlaylistGenerator.generate(theme, candidates);

    aiPlaylistGenerationValidator.validate(result, candidates);

    if (playlistRepository.existsByOwner_IdAndTitle(ownerId, result.getTitle())) {
      throw new InvalidAiPlaylistTitleException(
          "동일한 제목의 자동 생성 플레이리스트가 이미 존재합니다."
      );
    }

    aiPlaylistPersistenceService.save(
        ownerId,
        result.getTitle(),
        result.getDescription(),
        result.getContentIds(),
        result.getTags()
    );
  }
}
