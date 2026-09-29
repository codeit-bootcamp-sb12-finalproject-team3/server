package com.moduplaylist.api.watchparty.service;

import com.moduplaylist.core.user.entity.User;
import com.moduplaylist.core.user.repository.UserRepository;
import com.moduplaylist.core.watchparty.entity.WatchParty;
import com.moduplaylist.core.watchparty.entity.WatchPartyParticipant;
import com.moduplaylist.core.watchparty.entity.WatchPartyReminder;
import com.moduplaylist.core.watchparty.entity.WatchPartyStatus;
import com.moduplaylist.core.watchparty.exception.WatchPartyHostCannotSetReminderException;
import com.moduplaylist.core.watchparty.exception.WatchPartyInvalidStateException;
import com.moduplaylist.core.watchparty.exception.WatchPartyKickedCannotRejoinException;
import com.moduplaylist.core.watchparty.exception.WatchPartyNotFoundException;
import com.moduplaylist.core.watchparty.exception.WatchPartyReminderAlreadyExistsException;
import com.moduplaylist.core.watchparty.exception.WatchPartyReminderNotFoundException;
import com.moduplaylist.core.watchparty.repository.WatchPartyParticipantRepository;
import com.moduplaylist.core.watchparty.repository.WatchPartyReminderRepository;
import com.moduplaylist.core.watchparty.repository.WatchPartyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class WatchPartyReminderServiceTest {

    @Mock
    private WatchPartyRepository watchPartyRepository;
    @Mock
    private WatchPartyReminderRepository watchPartyReminderRepository;
    @Mock
    private WatchPartyParticipantRepository watchPartyParticipantRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WatchPartyReminderService watchPartyReminderService;

    private UUID partyId;
    private UUID userId;
    private UUID hostId;
    private User user;
    private User host;
    private WatchParty watchParty;

    @BeforeEach
    void setUp() {
        partyId = UUID.randomUUID();
        userId = UUID.randomUUID();
        hostId = UUID.randomUUID();

        user = User.create("guest@test.com", "encodedPw", "게스트");
        ReflectionTestUtils.setField(user, "id", userId);
        host = User.create("host@test.com", "encodedPw", "방장");
        ReflectionTestUtils.setField(host, "id", hostId);

        watchParty = Mockito.mock(WatchParty.class);
    }

    // 시작 대기(SCHEDULED) 상태 + 방장 정보까지 갖춘 파티 준비
    private void givenScheduledParty() {
        given(watchPartyRepository.findById(partyId)).willReturn(Optional.of(watchParty));
        given(watchParty.getStatus()).willReturn(WatchPartyStatus.SCHEDULED);
        given(watchParty.getScheduledAt()).willReturn(Instant.now().plus(1, ChronoUnit.DAYS));
        given(watchParty.getHost()).willReturn(host);
    }

    // ===== setReminder() =====

    // 케이스 1: 정상 설정 — 파티·사용자가 맞는 알림이 저장됨
    @Test
    void setReminder_정상_설정() {
        givenScheduledParty();
        given(watchPartyParticipantRepository.findByUser_IdAndWatchParty_Id(userId, partyId))
                .willReturn(Optional.empty());
        given(watchPartyReminderRepository.existsByWatchParty_IdAndUser_Id(partyId, userId))
                .willReturn(false);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        watchPartyReminderService.setReminder(partyId, userId);

        ArgumentCaptor<WatchPartyReminder> captor = ArgumentCaptor.forClass(WatchPartyReminder.class);
        then(watchPartyReminderRepository).should().saveAndFlush(captor.capture());
        assertThat(captor.getValue().getWatchParty()).isSameAs(watchParty);
        assertThat(captor.getValue().getUser()).isSameAs(user);
    }

    // 케이스 2: 파티가 존재하지 않으면 예외
    @Test
    void setReminder_파티없으면_예외() {
        given(watchPartyRepository.findById(partyId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> watchPartyReminderService.setReminder(partyId, userId))
                .isInstanceOf(WatchPartyNotFoundException.class);
        then(watchPartyReminderRepository).should(never()).saveAndFlush(any());
    }

    // 케이스 3: 시작 대기 상태가 아니면(LIVE, ENDED) 예외
    @ParameterizedTest
    @EnumSource(value = WatchPartyStatus.class, names = {"LIVE", "ENDED"})
    void setReminder_SCHEDULED_아니면_예외(WatchPartyStatus status) {
        given(watchPartyRepository.findById(partyId)).willReturn(Optional.of(watchParty));
        given(watchParty.getStatus()).willReturn(status);

        assertThatThrownBy(() -> watchPartyReminderService.setReminder(partyId, userId))
                .isInstanceOf(WatchPartyInvalidStateException.class);
        then(watchPartyReminderRepository).should(never()).saveAndFlush(any());
    }

    // 케이스 3-1: 시작 예정 시각이 이미 지난 방이면 예외
    @Test
    void setReminder_시작시각_지났으면_예외() {
        given(watchPartyRepository.findById(partyId)).willReturn(Optional.of(watchParty));
        given(watchParty.getStatus()).willReturn(WatchPartyStatus.SCHEDULED);
        given(watchParty.getScheduledAt()).willReturn(Instant.now().minus(1, ChronoUnit.HOURS));

        assertThatThrownBy(() -> watchPartyReminderService.setReminder(partyId, userId))
                .isInstanceOf(WatchPartyInvalidStateException.class);
        then(watchPartyReminderRepository).should(never()).saveAndFlush(any());
    }

    // 케이스 4: 방장은 알림 설정 불가
    @Test
    void setReminder_방장이면_예외() {
        givenScheduledParty();

        assertThatThrownBy(() -> watchPartyReminderService.setReminder(partyId, hostId))
                .isInstanceOf(WatchPartyHostCannotSetReminderException.class);
        then(watchPartyReminderRepository).should(never()).saveAndFlush(any());
    }

    // 케이스 5: 강퇴된 사용자는 알림 설정 불가
    @Test
    void setReminder_강퇴된_사용자면_예외() {
        givenScheduledParty();
        WatchPartyParticipant kicked = new WatchPartyParticipant(user, watchParty);
        kicked.kick();
        given(watchPartyParticipantRepository.findByUser_IdAndWatchParty_Id(userId, partyId))
                .willReturn(Optional.of(kicked));

        assertThatThrownBy(() -> watchPartyReminderService.setReminder(partyId, userId))
                .isInstanceOf(WatchPartyKickedCannotRejoinException.class);
        then(watchPartyReminderRepository).should(never()).saveAndFlush(any());
    }

    // 케이스 6: 이미 참가(JOINED) 중인 사용자는 설정 가능
    @Test
    void setReminder_참가중인_사용자는_설정_가능() {
        givenScheduledParty();
        WatchPartyParticipant joined = new WatchPartyParticipant(user, watchParty);
        given(watchPartyParticipantRepository.findByUser_IdAndWatchParty_Id(userId, partyId))
                .willReturn(Optional.of(joined));
        given(watchPartyReminderRepository.existsByWatchParty_IdAndUser_Id(partyId, userId))
                .willReturn(false);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        watchPartyReminderService.setReminder(partyId, userId);

        then(watchPartyReminderRepository).should().saveAndFlush(any(WatchPartyReminder.class));
    }

    // 케이스 7: 이미 설정한 알림이 있으면 예외
    @Test
    void setReminder_이미_설정했으면_예외() {
        givenScheduledParty();
        given(watchPartyParticipantRepository.findByUser_IdAndWatchParty_Id(userId, partyId))
                .willReturn(Optional.empty());
        given(watchPartyReminderRepository.existsByWatchParty_IdAndUser_Id(partyId, userId))
                .willReturn(true);

        assertThatThrownBy(() -> watchPartyReminderService.setReminder(partyId, userId))
                .isInstanceOf(WatchPartyReminderAlreadyExistsException.class);
        then(watchPartyReminderRepository).should(never()).saveAndFlush(any());
    }

    // 케이스 8: 동시 요청으로 UNIQUE 제약 위반 시 중복 예외로 변환 (원인 예외 보존)
    @Test
    void setReminder_동시요청_UNIQUE_위반이면_중복_예외() {
        givenScheduledParty();
        given(watchPartyParticipantRepository.findByUser_IdAndWatchParty_Id(userId, partyId))
                .willReturn(Optional.empty());
        given(watchPartyReminderRepository.existsByWatchParty_IdAndUser_Id(partyId, userId))
                .willReturn(false);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(watchPartyReminderRepository.saveAndFlush(any(WatchPartyReminder.class)))
                .willThrow(new DataIntegrityViolationException("uk_watch_party_reminders"));

        assertThatThrownBy(() -> watchPartyReminderService.setReminder(partyId, userId))
                .isInstanceOf(WatchPartyReminderAlreadyExistsException.class)
                .hasCauseInstanceOf(DataIntegrityViolationException.class);
    }

    // ===== cancelReminder() =====

    // 케이스 9: 정상 해제 — 1건 삭제되면 예외 없음
    @Test
    void cancelReminder_정상_해제() {
        given(watchPartyRepository.existsById(partyId)).willReturn(true);
        given(watchPartyReminderRepository.deleteByWatchPartyIdAndUserId(partyId, userId))
                .willReturn(1);

        assertThatCode(() -> watchPartyReminderService.cancelReminder(partyId, userId))
                .doesNotThrowAnyException();
    }

    // 케이스 10: 파티가 존재하지 않으면 예외, 삭제 쿼리는 실행 안 함
    @Test
    void cancelReminder_파티없으면_예외() {
        given(watchPartyRepository.existsById(partyId)).willReturn(false);

        assertThatThrownBy(() -> watchPartyReminderService.cancelReminder(partyId, userId))
                .isInstanceOf(WatchPartyNotFoundException.class);
        then(watchPartyReminderRepository).should(never())
                .deleteByWatchPartyIdAndUserId(any(), any());
    }

    // 케이스 11: 설정된 알림이 없으면(삭제 0건) 예외
    @Test
    void cancelReminder_알림없으면_예외() {
        given(watchPartyRepository.existsById(partyId)).willReturn(true);
        given(watchPartyReminderRepository.deleteByWatchPartyIdAndUserId(partyId, userId))
                .willReturn(0);

        assertThatThrownBy(() -> watchPartyReminderService.cancelReminder(partyId, userId))
                .isInstanceOf(WatchPartyReminderNotFoundException.class);
    }
}