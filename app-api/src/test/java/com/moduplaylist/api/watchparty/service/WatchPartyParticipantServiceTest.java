package com.moduplaylist.api.watchparty.service;

import com.moduplaylist.api.watchparty.event.WatchPartyParticipantChangedEvent;
import com.moduplaylist.core.user.entity.User;
import com.moduplaylist.core.user.repository.UserRepository;
import com.moduplaylist.core.watchparty.entity.ParticipantStatus;
import com.moduplaylist.core.watchparty.entity.WatchParty;
import com.moduplaylist.core.watchparty.entity.WatchPartyParticipant;
import com.moduplaylist.core.watchparty.entity.WatchPartyStatus;
import com.moduplaylist.core.watchparty.exception.WatchPartyAlreadyEndedException;
import com.moduplaylist.core.watchparty.exception.WatchPartyCapacityFullException;
import com.moduplaylist.core.watchparty.exception.WatchPartyKickedCannotRejoinException;
import com.moduplaylist.core.watchparty.exception.WatchPartyLobbyNotOpenException;
import com.moduplaylist.core.watchparty.repository.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WatchPartyParticipantServiceTest {

    @Mock private WatchPartyRepository watchPartyRepository;
    @Mock private UserRepository userRepository;
    @Mock private WatchPartyParticipantRepository participantRepository;
    @Mock private WatchPartyKickedRegistry kickedRegistry;
    @Mock private WatchPartyJoinedRegistry joinedRegistry;
    @Mock private WatchPartyActivePartyRegistry activePartyRegistry;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private WatchPartyGhostCleaner ghostCleaner;

    @InjectMocks private WatchPartyParticipantService service;

    private UUID partyId;
    private UUID userId;
    private User user;
    private WatchParty party;

    @BeforeEach
    void setUp() {
        partyId = UUID.randomUUID();
        userId = UUID.randomUUID();
        user = User.create("guest@test.com", "encodedPw", "guest");
        ReflectionTestUtils.setField(user, "id", userId);
        party = mock(WatchParty.class);
    }

    @Test
    void joinsWithoutPreviousParticipant() {
        givenJoinableParty(10, Optional.empty());

        service.joinWatchParty(partyId, userId);

        verify(participantRepository).save(any(WatchPartyParticipant.class));
        verify(joinedRegistry).join(partyId, userId);
        verify(activePartyRegistry).setJoinedParty(userId, partyId);
    }

    @Test
    void joiningSamePartyIsIdempotentAndRepairsRedis() {
        WatchPartyParticipant existing = new WatchPartyParticipant(user, party);
        givenJoinableParty(10, Optional.of(existing));

        service.joinWatchParty(partyId, userId);

        verify(participantRepository, never()).save(any());
        verify(participantRepository, never()).countByWatchParty_IdAndStatus(any(), any());
        verifyNoInteractions(eventPublisher);
        verify(joinedRegistry).join(partyId, userId);
        verify(activePartyRegistry).setJoinedParty(userId, partyId);
    }

    @Test
    void automaticallySwitchesFromAtoBWithoutSwitchFrom() {
        UUID fromPartyId = UUID.randomUUID();
        WatchPartyParticipant source = givenJoinedElsewhere(fromPartyId);
        givenJoinableParty(10, Optional.empty());
        given(participantRepository.markLeftIfJoined(eq(source.getId()), any())).willReturn(1);

        service.joinWatchParty(partyId, userId);

        InOrder order = inOrder(participantRepository, joinedRegistry, activePartyRegistry);
        order.verify(participantRepository).markLeftIfJoined(eq(source.getId()), any());
        order.verify(participantRepository).save(any(WatchPartyParticipant.class));
        order.verify(joinedRegistry).leave(fromPartyId, userId);
        order.verify(joinedRegistry).join(partyId, userId);
        order.verify(activePartyRegistry).setJoinedParty(userId, partyId);
        verify(activePartyRegistry, never()).clearJoinedParty(any());

        ArgumentCaptor<WatchPartyParticipantChangedEvent> events =
                ArgumentCaptor.forClass(WatchPartyParticipantChangedEvent.class);
        verify(eventPublisher, times(2)).publishEvent(events.capture());
        assertThat(events.getAllValues())
                .extracting(WatchPartyParticipantChangedEvent::partyId, WatchPartyParticipantChangedEvent::status)
                .containsExactly(tuple(fromPartyId, ParticipantStatus.LEFT), tuple(partyId, ParticipantStatus.JOINED));
    }

    @Test
    void fullDestinationDoesNotLeaveCurrentParty() {
        givenJoinedElsewhere(UUID.randomUUID());
        givenJoinableParty(10, Optional.empty());
        given(participantRepository.countByWatchParty_IdAndStatus(partyId, ParticipantStatus.JOINED))
                .willReturn(10L);

        assertThatThrownBy(() -> service.joinWatchParty(partyId, userId))
                .isInstanceOf(WatchPartyCapacityFullException.class);

        verify(participantRepository, never()).markLeftIfJoined(any(), any());
        verifyNoInteractions(joinedRegistry, activePartyRegistry, eventPublisher);
    }

    @Test
    void endedDestinationDoesNotLeaveCurrentParty() {
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(watchPartyRepository.findByIdForUpdate(partyId)).willReturn(Optional.of(party));
        given(party.getStatus()).willReturn(WatchPartyStatus.ENDED);

        assertThatThrownBy(() -> service.joinWatchParty(partyId, userId))
                .isInstanceOf(WatchPartyAlreadyEndedException.class);

        verify(participantRepository, never()).markLeftIfJoined(any(), any());
        verifyNoInteractions(joinedRegistry, activePartyRegistry, eventPublisher);
    }

    @Test
    void cannotJoinBeforeLobbyOpens() {
        ReflectionTestUtils.setField(service, "lobbyOpenMinutes", 30L);
        User host = User.create("host@test.com", "encodedPw", "host");
        ReflectionTestUtils.setField(host, "id", UUID.randomUUID());
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(watchPartyRepository.findByIdForUpdate(partyId)).willReturn(Optional.of(party));
        given(party.getStatus()).willReturn(WatchPartyStatus.SCHEDULED);
        given(party.getHost()).willReturn(host);
        given(party.getScheduledAt()).willReturn(Instant.now().plus(Duration.ofMinutes(31)));

        assertThatThrownBy(() -> service.joinWatchParty(partyId, userId))
                .isInstanceOf(WatchPartyLobbyNotOpenException.class);

        verify(participantRepository, never()).save(any());
        verifyNoInteractions(joinedRegistry, activePartyRegistry, eventPublisher);
    }

    @Test
    void canJoinWithinLobbyWindow() {
        ReflectionTestUtils.setField(service, "lobbyOpenMinutes", 30L);
        givenJoinableParty(10, Optional.empty());
        given(party.getStatus()).willReturn(WatchPartyStatus.SCHEDULED);
        given(party.getScheduledAt()).willReturn(Instant.now().plus(Duration.ofMinutes(29)));

        service.joinWatchParty(partyId, userId);

        verify(participantRepository).save(any(WatchPartyParticipant.class));
    }

    @Test
    void kickedDestinationDoesNotLeaveCurrentParty() {
        given(kickedRegistry.isKicked(partyId, userId)).willReturn(true);

        assertThatThrownBy(() -> service.joinWatchParty(partyId, userId))
                .isInstanceOf(WatchPartyKickedCannotRejoinException.class);

        verifyNoInteractions(userRepository, participantRepository, joinedRegistry, activePartyRegistry);
    }

    @Test
    void rejoiningLeftParticipantReusesRecord() {
        WatchPartyParticipant existing = new WatchPartyParticipant(user, party);
        existing.leave();
        givenJoinableParty(10, Optional.of(existing));

        service.joinWatchParty(partyId, userId);

        assertThat(existing.getStatus()).isEqualTo(ParticipantStatus.JOINED);
        verify(participantRepository, never()).save(any());
        verify(joinedRegistry).join(partyId, userId);
    }

    @Test
    void idempotentJoinAlsoLeavesInconsistentOtherJoinedParty() {
        WatchPartyParticipant existing = new WatchPartyParticipant(user, party);
        UUID fromPartyId = UUID.randomUUID();
        WatchPartyParticipant source = givenJoinedElsewhere(fromPartyId);
        givenJoinableParty(10, Optional.of(existing));
        given(participantRepository.markLeftIfJoined(eq(source.getId()), any())).willReturn(1);

        service.joinWatchParty(partyId, userId);

        verify(joinedRegistry).leave(fromPartyId, userId);
        verify(joinedRegistry).join(partyId, userId);
        verify(activePartyRegistry).setJoinedParty(userId, partyId);
    }

    @Test
    void keepsGhostCleanupForCapacityRecovery() {
        givenJoinableParty(10, Optional.empty());
        given(participantRepository.countByWatchParty_IdAndStatus(partyId, ParticipantStatus.JOINED))
                .willReturn(10L, 9L);
        given(ghostCleaner.cleanUpGhostsInParty(partyId)).willReturn(1);

        service.joinWatchParty(partyId, userId);

        verify(ghostCleaner).cleanUpGhostsInParty(partyId);
        verify(participantRepository).save(any());
    }

    @Test
    void switchDoesNotConsultGhostStatus() {
        WatchPartyParticipant source = givenJoinedElsewhere(UUID.randomUUID());
        givenJoinableParty(10, Optional.empty());
        given(participantRepository.markLeftIfJoined(eq(source.getId()), any())).willReturn(1);

        service.joinWatchParty(partyId, userId);

        verify(ghostCleaner, never()).cleanUpIfGhost(any());
    }

    @Test
    void getParticipantsReturnsJoinedParticipants() {
        given(watchPartyRepository.existsById(partyId)).willReturn(true);
        WatchPartyParticipant participant = new WatchPartyParticipant(user, party);
        given(participantRepository.findJoinedParticipants(partyId, ParticipantStatus.JOINED))
                .willReturn(List.of(participant));

        assertThat(service.getParticipants(partyId)).hasSize(1);

    }

    private void givenJoinableParty(int maxParticipants, Optional<WatchPartyParticipant> existing) {
        User host = User.create("host@test.com", "encodedPw", "host");
        ReflectionTestUtils.setField(host, "id", UUID.randomUUID());
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(watchPartyRepository.findByIdForUpdate(partyId)).willReturn(Optional.of(party));
        lenient().when(party.getId()).thenReturn(partyId);
        given(party.getStatus()).willReturn(WatchPartyStatus.LIVE);
        given(party.getHost()).willReturn(host);
        lenient().when(party.getMaxParticipants()).thenReturn(maxParticipants);
        given(participantRepository.findByUser_IdAndWatchParty_Id(userId, partyId)).willReturn(existing);
        lenient().when(participantRepository.countByWatchParty_IdAndStatus(partyId, ParticipantStatus.JOINED))
                .thenReturn(0L);
    }

    private WatchPartyParticipant givenJoinedElsewhere(UUID otherPartyId) {
        WatchParty otherParty = mock(WatchParty.class);
        lenient().when(otherParty.getId()).thenReturn(otherPartyId);
        WatchPartyParticipant participant = new WatchPartyParticipant(user, otherParty);
        ReflectionTestUtils.setField(participant, "id", UUID.randomUUID());
        lenient().when(participantRepository.findFirstByUser_IdAndStatusAndWatchParty_IdNot(
                userId, ParticipantStatus.JOINED, partyId))
                .thenReturn(Optional.of(participant));
        return participant;
    }
}
