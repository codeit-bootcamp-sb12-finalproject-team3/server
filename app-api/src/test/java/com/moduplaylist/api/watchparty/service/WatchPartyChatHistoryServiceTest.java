package com.moduplaylist.api.watchparty.service;

import com.moduplaylist.api.watchparty.dto.WatchPartyChatMessageResponse;
import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.common.exception.ErrorCode;
import com.moduplaylist.core.user.entity.User;
import com.moduplaylist.core.watchparty.entity.WatchParty;
import com.moduplaylist.core.watchparty.entity.WatchPartyParticipant;
import com.moduplaylist.core.watchparty.exception.WatchPartyChatAccessDeniedException;
import com.moduplaylist.core.watchparty.exception.WatchPartyNotFoundException;
import com.moduplaylist.core.watchparty.repository.WatchPartyChatLogRegistry;
import com.moduplaylist.core.watchparty.repository.WatchPartyChatMessage;
import com.moduplaylist.core.watchparty.repository.WatchPartyParticipantRepository;
import com.moduplaylist.core.watchparty.repository.WatchPartyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WatchPartyChatHistoryServiceTest {

    @Mock private WatchPartyRepository watchPartyRepository;
    @Mock private WatchPartyParticipantRepository watchPartyParticipantRepository;
    @Mock private WatchPartyChatLogRegistry watchPartyChatLogRegistry;

    @InjectMocks
    private WatchPartyChatHistoryService watchPartyChatHistoryService;

    private UUID partyId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        partyId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    // 케이스 1: 참가 기록이 없는 구경꾼도 조회 가능 + epoch millis → Instant 변환
    @Test
    void getRecentMessages_구경꾼_정상_조회() {
        UUID senderId = UUID.randomUUID();
        long sentAtMillis = 1_759_734_000_000L;

        given(watchPartyRepository.existsById(partyId)).willReturn(true);
        given(watchPartyParticipantRepository.findByUser_IdAndWatchParty_Id(userId, partyId))
                .willReturn(Optional.empty());
        given(watchPartyChatLogRegistry.findRecent(partyId, 50))
                .willReturn(List.of(new WatchPartyChatMessage(senderId, "안녕하세요", sentAtMillis)));

        List<WatchPartyChatMessageResponse> result =
                watchPartyChatHistoryService.getRecentMessages(partyId, userId, 50);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSenderId()).isEqualTo(senderId);
        assertThat(result.get(0).getContent()).isEqualTo("안녕하세요");
        assertThat(result.get(0).getSentAt()).isEqualTo(Instant.ofEpochMilli(sentAtMillis));
    }

    // 케이스 2: 강퇴된 사용자는 403 예외, Redis는 조회하지 않음
    @Test
    void getRecentMessages_강퇴자_예외() {
        User user = User.create("kicked@test.com", "encodedPw", "강퇴자");
        WatchPartyParticipant participant =
                new WatchPartyParticipant(user, Mockito.mock(WatchParty.class));
        participant.kick(); // JOINED → KICKED

        given(watchPartyRepository.existsById(partyId)).willReturn(true);
        given(watchPartyParticipantRepository.findByUser_IdAndWatchParty_Id(userId, partyId))
                .willReturn(Optional.of(participant));

        assertThatThrownBy(() -> watchPartyChatHistoryService.getRecentMessages(partyId, userId, 50))
                .isInstanceOf(WatchPartyChatAccessDeniedException.class);

        verify(watchPartyChatLogRegistry, never()).findRecent(any(), anyLong());
    }

    // 케이스 3: 파티가 없으면 404 예외
    @Test
    void getRecentMessages_파티없음_예외() {
        given(watchPartyRepository.existsById(partyId)).willReturn(false);

        assertThatThrownBy(() -> watchPartyChatHistoryService.getRecentMessages(partyId, userId, 50))
                .isInstanceOf(WatchPartyNotFoundException.class);
    }

    // 케이스 4: limit 범위 밖이면 400 예외, DB도 조회하지 않음
    @ParameterizedTest
    @ValueSource(ints = {0, -1, 501})
    void getRecentMessages_limit_범위밖_예외(int limit) {
        assertThatThrownBy(() -> watchPartyChatHistoryService.getRecentMessages(partyId, userId, limit))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REQUEST);

        verify(watchPartyRepository, never()).existsById(any());
    }

    // 케이스 5: sentAt 필드가 없는(null) 메시지가 섞여도 NPE로 전체 조회가 실패하지 않음
    @Test
    void getRecentMessages_sentAt_null_방어() {
        given(watchPartyRepository.existsById(partyId)).willReturn(true);
        given(watchPartyParticipantRepository.findByUser_IdAndWatchParty_Id(userId, partyId))
                .willReturn(Optional.empty());
        given(watchPartyChatLogRegistry.findRecent(partyId, 50))
                .willReturn(List.of(new WatchPartyChatMessage(UUID.randomUUID(), "내용", null)));

        List<WatchPartyChatMessageResponse> result =
                watchPartyChatHistoryService.getRecentMessages(partyId, userId, 50);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSentAt()).isNull();
    }
}