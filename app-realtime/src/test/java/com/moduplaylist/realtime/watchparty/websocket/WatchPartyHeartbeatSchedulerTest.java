package com.moduplaylist.realtime.watchparty.websocket;

import com.moduplaylist.realtime.watchparty.WatchPartyLastSeenRegistry;
import com.moduplaylist.realtime.watchparty.WatchPartyPlaybackRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.user.SimpSession;
import org.springframework.messaging.simp.user.SimpSubscription;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WatchPartyHeartbeatSchedulerTest {

    @Mock private SimpUserRegistry simpUserRegistry;
    @Mock private WatchPartyLastSeenRegistry lastSeenRegistry;
    @Mock private WatchPartyPlaybackRegistry playbackRegistry;

    @InjectMocks
    private WatchPartyHeartbeatScheduler watchPartyHeartbeatScheduler;

    private UUID partyId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        partyId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    // ===== heartbeat() =====

    // 케이스 1: 파티 구독만 모아서 기록 — 여러 탭·여러 채널은 한 번으로 합쳐지고, 다른 구독은 무시
    @Test
    void heartbeat_파티_구독자만_기록() {
        SimpSession tab1 = session(
                subscription("/sub/watch-parties/" + partyId + "/chat"),
                subscription("/sub/watch-parties/" + partyId + "/playback"));
        SimpSession tab2 = session(
                subscription("/sub/watch-parties/" + partyId + "/participants"),
                subscription("/user/queue/errors")); // 파티 구독 아님
        givenUsers(user(userId.toString(), tab1, tab2));

        watchPartyHeartbeatScheduler.heartbeat();

        verify(lastSeenRegistry).touchAll(eq(Map.of(partyId, Set.of(userId))), anyLong());
    }

    // 케이스 2: 파티 구독자가 없으면 — Redis에 쓰지 않음
    @Test
    void heartbeat_파티_구독자_없으면_기록안함() {
        givenUsers(user(userId.toString(), session(subscription("/user/queue/errors"))));

        watchPartyHeartbeatScheduler.heartbeat();

        verify(lastSeenRegistry, never()).touchAll(any(), anyLong());
    }

    // 케이스 3: partyId나 userId가 UUID 형식이 아니면 — 그 항목만 건너뜀
    @Test
    void heartbeat_잘못된_UUID는_건너뜀() {
        SimpUser validUser = user(userId.toString(), session(
                subscription("/sub/watch-parties/not-a-uuid/chat"),
                subscription("/sub/watch-parties/" + partyId + "/chat")));
        SimpUser invalidUser = user("not-a-uuid"); // 이름이 잘못되면 세션은 보지도 않으므로 세션 없이 생성
        givenUsers(validUser, invalidUser);

        watchPartyHeartbeatScheduler.heartbeat();

        verify(lastSeenRegistry).touchAll(eq(Map.of(partyId, Set.of(userId))), anyLong());
    }

    // 케이스 4: Redis 기록이 실패해도 — 예외를 밖으로 던지지 않음 (다음 틱에 재시도)
    @Test
    void heartbeat_Redis_실패해도_예외없음() {
        givenUsers(user(userId.toString(), session(subscription("/sub/watch-parties/" + partyId + "/chat"))));
        willThrow(new IllegalStateException("redis down")).given(lastSeenRegistry).touchAll(any(), anyLong());

        assertThatCode(() -> watchPartyHeartbeatScheduler.heartbeat()).doesNotThrowAnyException();
    }

    // 케이스 5: 종료된 파티는 — 구독이 남아 있어도 기록에서 제외
    @Test
    void heartbeat_종료된_파티는_제외() {
        UUID endedPartyId = UUID.randomUUID();
        givenUsers(user(userId.toString(), session(
                subscription("/sub/watch-parties/" + partyId + "/chat"),
                subscription("/sub/watch-parties/" + endedPartyId + "/chat"))));
        given(playbackRegistry.isEnded(partyId)).willReturn(false);
        given(playbackRegistry.isEnded(endedPartyId)).willReturn(true);

        watchPartyHeartbeatScheduler.heartbeat();

        verify(lastSeenRegistry).touchAll(eq(Map.of(partyId, Set.of(userId))), anyLong());
    }

    // 케이스 6: 모두 종료된 파티면 — Redis에 쓰지 않음
    @Test
    void heartbeat_모두_종료된_파티면_기록안함() {
        givenUsers(user(userId.toString(), session(subscription("/sub/watch-parties/" + partyId + "/chat"))));
        given(playbackRegistry.isEnded(partyId)).willReturn(true);

        watchPartyHeartbeatScheduler.heartbeat();

        verify(lastSeenRegistry, never()).touchAll(any(), anyLong());
    }

    // 케이스 7: 종료 확인이 실패한 파티도 — 온라인으로 보고 기록 (확실하지 않으면 온라인)
    @Test
    void heartbeat_종료확인_실패해도_기록() {
        UUID brokenPartyId = UUID.randomUUID();
        givenUsers(user(userId.toString(), session(
                subscription("/sub/watch-parties/" + partyId + "/chat"),
                subscription("/sub/watch-parties/" + brokenPartyId + "/chat"))));
        given(playbackRegistry.isEnded(partyId)).willReturn(false);
        given(playbackRegistry.isEnded(brokenPartyId)).willThrow(new IllegalStateException("역직렬화 실패"));

        watchPartyHeartbeatScheduler.heartbeat();

        verify(lastSeenRegistry).touchAll(
                eq(Map.of(partyId, Set.of(userId), brokenPartyId, Set.of(userId))), anyLong());
    }

    // ---- 테스트용 mock 생성 도우미 ----

    private void givenUsers(SimpUser... users) {
        given(simpUserRegistry.getUsers()).willReturn(Set.of(users));
    }

    private SimpUser user(String name, SimpSession... sessions) {
        SimpUser user = mock(SimpUser.class);
        given(user.getName()).willReturn(name);
        lenient().when(user.getSessions()).thenReturn(Set.of(sessions)); // 이름이 잘못되면 세션은 안 봄
        return user;
    }

    private SimpSession session(SimpSubscription... subscriptions) {
        SimpSession session = mock(SimpSession.class);
        given(session.getSubscriptions()).willReturn(Set.of(subscriptions));
        return session;
    }

    private SimpSubscription subscription(String destination) {
        SimpSubscription subscription = mock(SimpSubscription.class);
        given(subscription.getDestination()).willReturn(destination);
        return subscription;
    }
}
