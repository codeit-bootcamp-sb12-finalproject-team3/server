package com.moduplaylist.api.watchparty.service;

import com.moduplaylist.api.watchparty.event.WatchPartyParticipantChangedEvent;
import com.moduplaylist.core.user.entity.User;
import com.moduplaylist.core.watchparty.entity.ParticipantStatus;
import com.moduplaylist.core.watchparty.entity.WatchParty;
import com.moduplaylist.core.watchparty.entity.WatchPartyParticipant;
import com.moduplaylist.core.watchparty.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WatchPartyGhostCleanerTest {

    @Mock private WatchPartyParticipantRepository watchPartyParticipantRepository;
    @Mock private WatchPartyLastSeenRegistry watchPartyLastSeenRegistry;
    @Mock private WatchPartyJoinedRegistry watchPartyJoinedRegistry;
    @Mock private WatchPartyActivePartyRegistry watchPartyActivePartyRegistry;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private WatchPartyGhostCleaner watchPartyGhostCleaner;

    private UUID partyId;
    private WatchParty watchParty;
    private Instant now;

    @BeforeEach
    void setUp() {
        partyId = UUID.randomUUID();
        watchParty = org.mockito.Mockito.mock(WatchParty.class);
        lenient().when(watchParty.getId()).thenReturn(partyId);
        now = Instant.parse("2026-09-28T10:00:00Z"); // isGhost 경계값 테스트용 고정 시각
    }

    // ===== isGhost() =====

    // 케이스 1: 마지막 확인이 정확히 5분 전 — 아직 온라인 (5분 "초과"부터 유령)
    @Test
    void isGhost_정확히_5분이면_온라인() {
        assertThat(WatchPartyGhostCleaner.isGhost(
                now.minus(Duration.ofMinutes(30)), now.minus(Duration.ofMinutes(5)), now)).isFalse();
    }

    // 케이스 2: 5분을 1ms라도 넘으면 — 유령
    @Test
    void isGhost_5분_초과면_유령() {
        assertThat(WatchPartyGhostCleaner.isGhost(
                now.minus(Duration.ofMinutes(30)), now.minus(Duration.ofMinutes(5).plusMillis(1)), now)).isTrue();
    }

    // 케이스 3: lastSeen이 없으면 — joinedAt 기준으로 판단
    @Test
    void isGhost_lastSeen없으면_joinedAt_기준() {
        assertThat(WatchPartyGhostCleaner.isGhost(now.minus(Duration.ofMinutes(3)), null, now)).isFalse();
        assertThat(WatchPartyGhostCleaner.isGhost(now.minus(Duration.ofMinutes(6)), null, now)).isTrue();
    }

    // 케이스 4: 재참가 직후 — 예전 lastSeen보다 새 joinedAt이 우선
    @Test
    void isGhost_재참가직후_joinedAt이_우선() {
        assertThat(WatchPartyGhostCleaner.isGhost(
                now.minus(Duration.ofMinutes(1)), now.minus(Duration.ofMinutes(30)), now)).isFalse();
    }

    // ===== cleanUpIfGhost() — 다른 방 확인 경로 =====

    // 케이스 5: 유령이면 — LEFT 처리 + 이벤트 + Redis 정리
    @Test
    void cleanUpIfGhost_유령이면_정리() {
        WatchPartyParticipant participant = participantJoinedAgo(Duration.ofMinutes(30));
        UUID userId = participant.getUser().getId();
        given(watchPartyLastSeenRegistry.findLastSeen(partyId, userId))
                .willReturn(Optional.of(Instant.now().minus(Duration.ofMinutes(10))));
        given(watchPartyParticipantRepository.markLeftIfJoined(eq(participant.getId()), any(Instant.class)))
                .willReturn(1);

        assertThat(watchPartyGhostCleaner.cleanUpIfGhost(participant)).isTrue();

        ArgumentCaptor<WatchPartyParticipantChangedEvent> captor =
                ArgumentCaptor.forClass(WatchPartyParticipantChangedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().partyId()).isEqualTo(partyId);
        assertThat(captor.getValue().userId()).isEqualTo(userId);
        assertThat(captor.getValue().status()).isEqualTo(ParticipantStatus.LEFT);
        verify(watchPartyJoinedRegistry).leave(partyId, userId);
        verify(watchPartyActivePartyRegistry).clearJoinedParty(userId);
    }

    // 케이스 6: 다른 경로가 먼저 정리했으면 — true지만 이벤트·Redis는 건드리지 않음 (LEFT 방송 중복 방지)
    @Test
    void cleanUpIfGhost_이미_정리됐으면_이벤트_없음() {
        WatchPartyParticipant participant = participantJoinedAgo(Duration.ofMinutes(30));
        given(watchPartyLastSeenRegistry.findLastSeen(partyId, participant.getUser().getId()))
                .willReturn(Optional.empty());
        given(watchPartyParticipantRepository.markLeftIfJoined(eq(participant.getId()), any(Instant.class)))
                .willReturn(0);

        assertThat(watchPartyGhostCleaner.cleanUpIfGhost(participant)).isTrue();

        verifyNoInteractions(eventPublisher, watchPartyJoinedRegistry, watchPartyActivePartyRegistry);
    }

    // 케이스 7: 최근에 확인됐으면 — 정리하지 않음
    @Test
    void cleanUpIfGhost_온라인이면_정리안함() {
        WatchPartyParticipant participant = participantJoinedAgo(Duration.ofMinutes(30));
        given(watchPartyLastSeenRegistry.findLastSeen(partyId, participant.getUser().getId()))
                .willReturn(Optional.of(Instant.now().minus(Duration.ofMinutes(1))));

        assertThat(watchPartyGhostCleaner.cleanUpIfGhost(participant)).isFalse();

        verify(watchPartyParticipantRepository, never()).markLeftIfJoined(any(), any());
    }

    // 케이스 8: lastSeen 조회 실패 — 온라인으로 간주, 정리하지 않음 (확실하지 않으면 온라인)
    @Test
    void cleanUpIfGhost_조회실패면_정리안함() {
        WatchPartyParticipant participant = participantJoinedAgo(Duration.ofMinutes(30));
        given(watchPartyLastSeenRegistry.findLastSeen(partyId, participant.getUser().getId()))
                .willThrow(new IllegalStateException("redis down"));

        assertThat(watchPartyGhostCleaner.cleanUpIfGhost(participant)).isFalse();

        verify(watchPartyParticipantRepository, never()).markLeftIfJoined(any(), any());
    }

    // ===== cleanUpGhostsInParty() — 정원 확인 / 스케줄러 경로 =====

    // 케이스 9: 온라인·유령·방금 참가한 사람이 섞인 방 — 유령만 정리, 정리 인원 반환
    @Test
    void cleanUpGhostsInParty_유령만_정리() {
        WatchPartyParticipant online = participantJoinedAgo(Duration.ofMinutes(30));
        WatchPartyParticipant ghost = participantJoinedAgo(Duration.ofMinutes(30));
        WatchPartyParticipant justJoined = participantJoinedAgo(Duration.ofMinutes(1)); // 아직 하트비트 전
        given(watchPartyParticipantRepository.findJoinedParticipants(partyId, ParticipantStatus.JOINED))
                .willReturn(List.of(online, ghost, justJoined));
        given(watchPartyLastSeenRegistry.findAllLastSeen(partyId)).willReturn(Map.of(
                online.getUser().getId(), Instant.now().minus(Duration.ofMinutes(1)),
                ghost.getUser().getId(), Instant.now().minus(Duration.ofMinutes(10))));
        given(watchPartyParticipantRepository.markLeftIfJoined(eq(ghost.getId()), any(Instant.class)))
                .willReturn(1);

        assertThat(watchPartyGhostCleaner.cleanUpGhostsInParty(partyId)).isEqualTo(1);

        verify(watchPartyParticipantRepository, times(1)).markLeftIfJoined(any(), any());
        verify(watchPartyJoinedRegistry).leave(partyId, ghost.getUser().getId());
    }

    // 케이스 10: 방 전체 lastSeen 조회 실패 — 아무도 정리하지 않음
    @Test
    void cleanUpGhostsInParty_조회실패면_정리안함() {
        given(watchPartyParticipantRepository.findJoinedParticipants(partyId, ParticipantStatus.JOINED))
                .willReturn(List.of(participantJoinedAgo(Duration.ofMinutes(30))));
        given(watchPartyLastSeenRegistry.findAllLastSeen(partyId))
                .willThrow(new IllegalStateException("redis down"));

        assertThat(watchPartyGhostCleaner.cleanUpGhostsInParty(partyId)).isZero();

        verify(watchPartyParticipantRepository, never()).markLeftIfJoined(any(), any());
    }

    // ---- 테스트용 도우미 ----

    // ago만큼 전에 참가한 JOINED 참가자 (사용자·참가 기록 id 포함)
    private WatchPartyParticipant participantJoinedAgo(Duration ago) {
        UUID userId = UUID.randomUUID();
        User user = User.create(userId + "@test.com", "encodedPw", "참가자");
        ReflectionTestUtils.setField(user, "id", userId);

        WatchPartyParticipant participant = new WatchPartyParticipant(user, watchParty);
        ReflectionTestUtils.setField(participant, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(participant, "joinedAt", Instant.now().minus(ago));
        return participant;
    }
}