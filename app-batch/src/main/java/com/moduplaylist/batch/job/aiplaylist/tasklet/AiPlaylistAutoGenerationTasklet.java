package com.moduplaylist.batch.job.aiplaylist.tasklet;

import com.moduplaylist.batch.job.aiplaylist.AiPlaylistAutoGenerationService;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class AiPlaylistAutoGenerationTasklet implements Tasklet {

  private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");

  private final AiPlaylistAutoGenerationService aiPlaylistAutoGenerationService;

  @Override
  public RepeatStatus execute(
      StepContribution contribution,
      ChunkContext chunkContext
  ) {
    LocalDate date = LocalDate.now(ZONE_ID);

    aiPlaylistAutoGenerationService.generateWeekly(date);

    log.info("AI 플레이리스트 주간 배치 Step 완료 - date={}", date);

    return RepeatStatus.FINISHED;
  }
}
