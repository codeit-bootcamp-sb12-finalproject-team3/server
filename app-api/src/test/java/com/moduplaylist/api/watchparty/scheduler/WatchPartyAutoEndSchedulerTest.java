package com.moduplaylist.api.watchparty.scheduler;

import com.moduplaylist.core.watchparty.entity.WatchPartyPlaybackStatus;
import com.moduplaylist.core.watchparty.repository.WatchPartyPlaybackState;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WatchPartyAutoEndSchedulerTest {

    private static final int PLANNED_MINUTES = 100;           // 예정 시간 100분 → ①은 110분, ②는 210분
    private static final Instant NOW = Instant.parse("2026-10-02T12:00:00Z");

    @Test
    void doesNotEndBeforePlannedPlusGrace() {
        WatchPartyPlaybackState state = live(minutesAgo(109), 0L);

        assertThat(shouldEnd(state, minutesAgo(109))).isFalse();
    }

    @Test
    void endsWhenPlayedTimeReachesPlannedPlusGrace() {
        WatchPartyPlaybackState state = live(minutesAgo(110), 0L);

        assertThat(shouldEnd(state, minutesAgo(110))).isTrue();
    }

    @Test
    void pausedTimeIsNotCountedAsPlayed() {
        // 130분 전에 시작했지만 30분 쉬었으므로 실제 재생은 100분
        WatchPartyPlaybackState state = live(minutesAgo(130), minutes(30));

        assertThat(shouldEnd(state, minutesAgo(130))).isFalse();
    }

    @Test
    void endsAbandonedPausedPartyAtUpperLimit() {
        // 50분 보고 일시정지한 채 방치. 예정 시각부터 210분(= 100 × 2 + 10)이 지남
        long startedAt = minutesAgo(210);
        WatchPartyPlaybackState state = paused(startedAt, startedAt + minutes(50));

        assertThat(shouldEnd(state, minutesAgo(210))).isTrue();
    }

    @Test
    void withoutPlaybackStateUsesUpperLimitOnly() {
        assertThat(shouldEnd(null, minutesAgo(200))).isFalse();
        assertThat(shouldEnd(null, minutesAgo(210))).isTrue();
    }

    // ===== 헬퍼 =====

    private boolean shouldEnd(WatchPartyPlaybackState state, long scheduledAtMs) {
        return WatchPartyAutoEndScheduler.shouldAutoEnd(
                state, PLANNED_MINUTES, Instant.ofEpochMilli(scheduledAtMs), NOW);
    }

    private WatchPartyPlaybackState live(long startedAt, long accumulatedPauseMs) {
        return new WatchPartyPlaybackState(WatchPartyPlaybackStatus.LIVE,
                startedAt, accumulatedPauseMs, null, null, null, UUID.randomUUID(), startedAt);
    }

    private WatchPartyPlaybackState paused(long startedAt, long pausedAt) {
        return new WatchPartyPlaybackState(WatchPartyPlaybackStatus.PAUSED,
                startedAt, 0L, pausedAt, null, null, UUID.randomUUID(), pausedAt);
    }

    private static long minutesAgo(int minutes) {
        return NOW.toEpochMilli() - minutes(minutes);
    }

    private static long minutes(int minutes) {
        return minutes * 60_000L;
    }
}