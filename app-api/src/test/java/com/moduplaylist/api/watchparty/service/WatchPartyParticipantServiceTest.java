package com.moduplaylist.api.watchparty.service;

import com.moduplaylist.api.watchparty.dto.WatchPartyParticipantResponse;
import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.user.entity.User;
import com.moduplaylist.core.user.repository.UserRepository;
import com.moduplaylist.core.watchparty.entity.ParticipantStatus;
import com.moduplaylist.core.watchparty.entity.WatchParty;
import com.moduplaylist.core.watchparty.entity.WatchPartyParticipant;
import com.moduplaylist.core.watchparty.entity.WatchPartyStatus;
import com.moduplaylist.core.watchparty.exception.WatchPartyAlreadyJoinedElsewhereException;
import com.moduplaylist.core.watchparty.exception.WatchPartyCapacityFullException;
import com.moduplaylist.core.watchparty.exception.WatchPartyNotFoundException;
import com.moduplaylist.core.watchparty.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WatchPartyParticipantServiceTest {

    @Mock private WatchPartyRepository watchPartyRepository;
    @Mock private UserRepository userRepository;
    @Mock private WatchPartyParticipantRepository watchPartyParticipantRepository;
    @Mock private WatchPartyKickedRegistry watchPartyKickedRegistry;
    @Mock private WatchPartyJoinedRegistry watchPartyJoinedRegistry;
    @Mock private WatchPartyActivePartyRegistry watchPartyActivePartyRegistry;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private WatchPartyGhostCleaner watchPartyGhostCleaner;

    @InjectMocks
    private WatchPartyParticipantService watchPartyParticipantService;

    private UUID partyId;
    private UUID userId;
    private User user;
    private WatchParty watchParty;

    @BeforeEach
    void setUp() {
        partyId = UUID.randomUUID();
        userId = UUID.randomUUID();
        user = User.create("guest@test.com", "encodedPw", "게스트");
        ReflectionTestUtils.setField(user, "id", userId);
        watchParty = org.mockito.Mockito.mock(WatchParty.class);
    }

    // ===== getParticipants() =====

    // 케이스 1: 정상 조회 — JOINED 상태 참가자만 매핑되어 반환됨
    @Test
    void getParticipants_정상_조회() {
        given(watchPartyRepository.existsById(partyId)).willReturn(true);

        WatchPartyParticipant participant = new WatchPartyParticipant(user, watchParty);
        given(watchPartyParticipantRepository.findJoinedParticipants(
                eq(partyId), eq(com.moduplaylist.core.watchparty.entity.ParticipantStatus.JOINED)))
                .willReturn(List.of(participant));

        List<WatchPartyParticipantResponse> result =
                watchPartyParticipantService.getParticipants(partyId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUser().getUserId()).isEqualTo(userId);
        assertThat(result.get(0).getUser().getName()).isEqualTo("게스트");
        assertThat(result.get(0).getJoinedAt()).isEqualTo(participant.getJoinedAt());
    }

    // 케이스 2: 파티가 존재하지 않으면 예외
    @Test
    void getParticipants_존재하지않으면_예외() {
        given(watchPartyRepository.existsById(partyId)).willReturn(false);

        assertThatThrownBy(() -> watchPartyParticipantService.getParticipants(partyId))
                .isInstanceOf(WatchPartyNotFoundException.class);
    }

    // ===== joinWatchParty() — #167 유령 정리 연결 =====

    // 케이스 3: 다른 방에 JOINED가 남아 있지만 유령이면 — 정리하고 참가 진행
    @Test
    void joinWatchParty_다른방_유령이면_정리하고_참가() {
        UUID otherPartyId = UUID.randomUUID();
        WatchPartyParticipant ghost = joinedElsewhere(otherPartyId);
        given(watchPartyParticipantRepository.findFirstByUser_IdAndStatusAndWatchParty_IdNotAndWatchParty_StatusNot(
                userId, ParticipantStatus.JOINED, partyId, WatchPartyStatus.ENDED))
                .willReturn(Optional.of(ghost));
        given(watchPartyGhostCleaner.cleanUpIfGhost(ghost)).willReturn(true);
        givenJoinableParty(10);
        given(watchPartyParticipantRepository.countByWatchParty_IdAndStatus(partyId, ParticipantStatus.JOINED))
                .willReturn(0L);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        watchPartyParticipantService.joinWatchParty(partyId, userId);

        verify(watchPartyParticipantRepository).save(any(WatchPartyParticipant.class));
        verify(watchPartyActivePartyRegistry).setJoinedParty(userId, partyId);
    }

    // 케이스 4: 다른 방에서 실제로 보고 있으면 — 차단 + 에러에 joinedPartyId
    @Test
    void joinWatchParty_다른방_온라인이면_joinedPartyId와_함께_예외() {
        UUID otherPartyId = UUID.randomUUID();
        WatchPartyParticipant online = joinedElsewhere(otherPartyId);
        given(watchPartyParticipantRepository.findFirstByUser_IdAndStatusAndWatchParty_IdNotAndWatchParty_StatusNot(
                userId, ParticipantStatus.JOINED, partyId, WatchPartyStatus.ENDED))
                .willReturn(Optional.of(online));
        given(watchPartyGhostCleaner.cleanUpIfGhost(online)).willReturn(false);

        assertThatThrownBy(() -> watchPartyParticipantService.joinWatchParty(partyId, userId))
                .isInstanceOf(WatchPartyAlreadyJoinedElsewhereException.class)
                .satisfies(e -> assertThat(((BaseException) e).getData())
                        .containsEntry("partyId", partyId)
                        .containsEntry("joinedPartyId", otherPartyId));

        verify(watchPartyRepository, never()).findByIdForUpdate(any());
    }

    // 케이스 5: 정원이 찼지만 유령이 있으면 — 정리 후 다시 세서 참가
    @Test
    void joinWatchParty_정원가득_유령정리후_참가() {
        givenJoinableParty(10);
        given(watchPartyParticipantRepository.countByWatchParty_IdAndStatus(partyId, ParticipantStatus.JOINED))
                .willReturn(10L, 9L); // 정리 전 10명 → 정리 후 9명
        given(watchPartyGhostCleaner.cleanUpGhostsInParty(partyId)).willReturn(1);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        watchPartyParticipantService.joinWatchParty(partyId, userId);

        verify(watchPartyParticipantRepository).save(any(WatchPartyParticipant.class));
    }

    // 케이스 6: 정원이 찼고 유령도 없으면 — 정원 초과 예외, 다시 세지 않음
    @Test
    void joinWatchParty_정원가득_유령없으면_예외() {
        givenJoinableParty(10);
        given(watchPartyParticipantRepository.countByWatchParty_IdAndStatus(partyId, ParticipantStatus.JOINED))
                .willReturn(10L);
        given(watchPartyGhostCleaner.cleanUpGhostsInParty(partyId)).willReturn(0);

        assertThatThrownBy(() -> watchPartyParticipantService.joinWatchParty(partyId, userId))
                .isInstanceOf(WatchPartyCapacityFullException.class);

        verify(watchPartyParticipantRepository, times(1))
                .countByWatchParty_IdAndStatus(partyId, ParticipantStatus.JOINED);
        verify(watchPartyParticipantRepository, never()).save(any());
    }

    // 케이스 7: 자리가 남아 있으면 — 유령 정리를 시도하지 않음
    @Test
    void joinWatchParty_자리있으면_유령정리_안함() {
        givenJoinableParty(10);
        given(watchPartyParticipantRepository.countByWatchParty_IdAndStatus(partyId, ParticipantStatus.JOINED))
                .willReturn(5L);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        watchPartyParticipantService.joinWatchParty(partyId, userId);

        verify(watchPartyGhostCleaner, never()).cleanUpGhostsInParty(any());
    }

    // ---- 테스트용 도우미 ----

    // 참가 가능한 파티: LIVE, 방장은 다른 사람, 이 사용자의 기존 참가 기록 없음
    private void givenJoinableParty(int maxParticipants) {
        User host = User.create("host@test.com", "encodedPw", "방장");
        ReflectionTestUtils.setField(host, "id", UUID.randomUUID());
        given(watchPartyRepository.findByIdForUpdate(partyId)).willReturn(Optional.of(watchParty));
        given(watchParty.getId()).willReturn(partyId);
        given(watchParty.getStatus()).willReturn(WatchPartyStatus.LIVE);
        given(watchParty.getHost()).willReturn(host);
        given(watchParty.getMaxParticipants()).willReturn(maxParticipants);
        given(watchPartyParticipantRepository.findByUser_IdAndWatchParty_Id(userId, partyId))
                .willReturn(Optional.empty());
    }

    // 다른 방(otherPartyId)에 JOINED로 남아 있는 이 사용자의 참가 기록
    private WatchPartyParticipant joinedElsewhere(UUID otherPartyId) {
        WatchParty otherParty = org.mockito.Mockito.mock(WatchParty.class);
        lenient().when(otherParty.getId()).thenReturn(otherPartyId);
        return new WatchPartyParticipant(user, otherParty);
    }
}