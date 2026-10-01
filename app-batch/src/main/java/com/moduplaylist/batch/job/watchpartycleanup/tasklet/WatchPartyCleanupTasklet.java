package com.moduplaylist.batch.job.watchpartycleanup.tasklet;

import com.moduplaylist.core.watchparty.entity.WatchPartyStatus;
import com.moduplaylist.core.watchparty.repository.WatchPartyKeyLifecycleRegistry;
import com.moduplaylist.core.watchparty.repository.WatchPartyRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@StepScope
public class WatchPartyCleanupTasklet implements Tasklet {

    private static final int DELETE_BATCH_SIZE = 500;

    private final WatchPartyRepository watchPartyRepository;
    private final WatchPartyKeyLifecycleRegistry watchPartyKeyLifecycleRegistry;
    private final Instant endedBefore;

    public WatchPartyCleanupTasklet(
            WatchPartyRepository watchPartyRepository,
            WatchPartyKeyLifecycleRegistry watchPartyKeyLifecycleRegistry,
            @Value("${mopl.batch.watch-party-cleanup.retention-days:14}") long retentionDays
    ) {
        this.watchPartyRepository = watchPartyRepository;
        this.watchPartyKeyLifecycleRegistry = watchPartyKeyLifecycleRegistry;
        // Step 실행 1번당 기준 시각을 한 번만 정한다 (묶음마다 기준이 바뀌지 않도록)
        this.endedBefore = Instant.now().minus(Duration.ofDays(retentionDays));
    }

    // 한 번 호출 = 최대 500개 삭제 = 트랜잭션 1개
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        List<UUID> partyIds = watchPartyRepository.findIdsByStatusAndEndedAtBefore(
                WatchPartyStatus.ENDED,
                endedBefore,
                PageRequest.of(0, DELETE_BATCH_SIZE)
        );
        if (partyIds.isEmpty()) {
            log.info("종료된 Watch Party 정리 완료 - endedBefore={}", endedBefore);
            return RepeatStatus.FINISHED;
        }

        int deleted = watchPartyRepository.deleteAllByIdIn(partyIds);
        partyIds.forEach(watchPartyKeyLifecycleRegistry::deletePartyKeysNow);
        contribution.incrementWriteCount(deleted);

        // 조회는 됐는데 하나도 안 지워졌다면 같은 id를 무한 반복하게 되므로 중단
        if (deleted == 0) {
            log.warn("종료된 Watch Party 삭제 중단 - 조회 {}건 중 삭제 0건", partyIds.size());
            return RepeatStatus.FINISHED;
        }

        log.info("종료된 Watch Party 묶음 삭제 - deleted={}", deleted);
        return RepeatStatus.CONTINUABLE;
    }
}