package com.moduplaylist.batch.job.playlistpopularity.tasklet;

import com.moduplaylist.core.playlist.repository.PlaylistRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@StepScope
public class PlaylistPopularityTasklet implements Tasklet {

  private final PlaylistRepository playlistRepository;
  private final BigDecimal totalSubscriberWeight;
  private final BigDecimal weeklyNewSubscriberWeight;

  public PlaylistPopularityTasklet(
      PlaylistRepository playlistRepository,
      @Value("${mopl.batch.playlist-popularity.total-subscriber-weight:0.3}")
      BigDecimal totalSubscriberWeight,
      @Value("${mopl.batch.playlist-popularity.weekly-new-subscriber-weight:0.7}")
      BigDecimal weeklyNewSubscriberWeight
  ) {
    this.playlistRepository = playlistRepository;
    this.totalSubscriberWeight = totalSubscriberWeight;
    this.weeklyNewSubscriberWeight = weeklyNewSubscriberWeight;
  }

  @Override
  public RepeatStatus execute(
      StepContribution contribution,
      ChunkContext chunkContext
  ) {
    Instant now = Instant.now();

    Instant weekStart =
        now.atZone(ZoneOffset.UTC)
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .toLocalDate()
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant();

    int updatedCount =
        playlistRepository.updateWeeklyPopularityScores(
            weekStart,
            totalSubscriberWeight,
            weeklyNewSubscriberWeight
        );

    log.info(
        "주간 인기 플레이리스트 점수 갱신 완료 - weekStart={}, updatedCount={}",
        weekStart,
        updatedCount
    );

    return RepeatStatus.FINISHED;
  }


}
