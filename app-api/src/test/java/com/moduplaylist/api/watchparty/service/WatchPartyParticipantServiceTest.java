package com.moduplaylist.api.watchparty.service;

import com.moduplaylist.api.watchparty.dto.WatchPartyParticipantResponse;
import com.moduplaylist.core.user.entity.User;
import com.moduplaylist.core.user.repository.UserRepository;
import com.moduplaylist.core.watchparty.entity.WatchParty;
import com.moduplaylist.core.watchparty.entity.WatchPartyParticipant;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class WatchPartyParticipantServiceTest {

    @Mock private WatchPartyRepository watchPartyRepository;
    @Mock private UserRepository userRepository;
    @Mock private WatchPartyParticipantRepository watchPartyParticipantRepository;
    @Mock private WatchPartyKickedRegistry watchPartyKickedRegistry;
    @Mock private WatchPartyJoinedRegistry watchPartyJoinedRegistry;
    @Mock private WatchPartyActivePartyRegistry watchPartyActivePartyRegistry;
    @Mock private ApplicationEventPublisher eventPublisher;

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
}