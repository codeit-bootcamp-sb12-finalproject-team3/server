package com.moduplaylist.api.watchparty.service;

import com.moduplaylist.api.watchparty.dto.CreateWatchPartyRequest;
import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.entity.ContentType;
import com.moduplaylist.core.user.entity.User;
import com.moduplaylist.core.watchparty.entity.WatchParty;
import com.moduplaylist.core.watchparty.exception.WatchPartyInvalidEpisodeRangeException;
import com.moduplaylist.core.watchparty.repository.*;
import com.moduplaylist.core.user.repository.UserRepository;
import com.moduplaylist.core.content.repository.ContentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class WatchPartyServiceTest {

    @Mock private WatchPartyRepository watchPartyRepository;
    @Mock private UserRepository userRepository;
    @Mock private ContentRepository contentRepository;
    @Mock private WatchPartyQueryRepository watchPartyQueryRepository;
    @Mock private WatchPartyParticipantRepository watchPartyParticipantRepository;
    @Mock private WatchPartyHostRegistry watchPartyHostRegistry;
    @Mock private WatchPartyPlaybackRegistry watchPartyPlaybackRegistry;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private WatchPartyReminderRepository watchPartyReminderRepository;

    @InjectMocks
    private WatchPartyService watchPartyService;

    private UUID hostId;
    private UUID contentId;
    private User host;

    @BeforeEach
    void setUp() {
        hostId = UUID.randomUUID();
        contentId = UUID.randomUUID();
        host = User.create("dawoon@test.com", "encodedPw", "다운");
    }

    private CreateWatchPartyRequest buildRequest(Integer startEpisode, Integer endEpisode) {
        return new CreateWatchPartyRequest(
                contentId,
                "파티 제목",
                null,                               // description
                Instant.now().plusSeconds(3600),    // scheduledAt
                4,                                   // maxParticipants
                120,                                 // sessionDurationMinutes
                startEpisode,
                endEpisode
        );
    }

    private Content buildTvSeasonContent(Integer episodeCount) {
        Content parentSeries = Content.builder()
                .title("시리즈 제목")
                .type(ContentType.TV_SERIES)
                .build();

        return Content.builder()
                .parentContent(parentSeries)
                .title("시즌 제목")
                .seasonNumber(1)
                .episodeCount(episodeCount)
                .type(ContentType.TV_SEASON)
                .thumbnailUrl("https://example.com/thumb.png")
                .build();
    }

    // 케이스 1: 회차 범위 없음 → 통과
    @Test
    void createWatchParty_회차범위없으면_통과() {
        // given
        Content content = Content.builder()
                .title("영화 제목")
                .type(ContentType.MOVIE)
                .thumbnailUrl("https://example.com/thumb.png")
                .build();

        CreateWatchPartyRequest request = buildRequest(null, null);

        given(userRepository.findById(hostId)).willReturn(Optional.of(host));
        given(contentRepository.findById(contentId)).willReturn(Optional.of(content));
        given(watchPartyRepository.save(any(WatchParty.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when & then
        assertThatCode(() -> watchPartyService.createWatchParty(hostId, request))
                .doesNotThrowAnyException();
    }

    // 케이스 2: TV_SEASON, 회차 범위가 episodeCount 이내 → 통과
    @Test
    void createWatchParty_회차범위가_episodeCount_이내면_통과() {
        // given
        Content content = buildTvSeasonContent(10);
        CreateWatchPartyRequest request = buildRequest(1, 10);

        given(userRepository.findById(hostId)).willReturn(Optional.of(host));
        given(contentRepository.findById(contentId)).willReturn(Optional.of(content));
        given(watchPartyRepository.save(any(WatchParty.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when & then
        assertThatCode(() -> watchPartyService.createWatchParty(hostId, request))
                .doesNotThrowAnyException();
    }

    // 케이스 3: 타입 불일치 (MOVIE인데 회차 범위 지정) → 예외
    @Test
    void createWatchParty_회차범위가_있는데_에피소드타입_아니면_예외() {
        // given
        Content content = Content.builder()
                .title("영화 제목")
                .type(ContentType.MOVIE)
                .thumbnailUrl("https://example.com/thumb.png")
                .build();

        CreateWatchPartyRequest request = buildRequest(1, 3);

        given(userRepository.findById(hostId)).willReturn(Optional.of(host));
        given(contentRepository.findById(contentId)).willReturn(Optional.of(content));

        // when & then
        assertThatThrownBy(() -> watchPartyService.createWatchParty(hostId, request))
                .isInstanceOf(WatchPartyInvalidEpisodeRangeException.class);
    }

    // 케이스 4 : 회차 범위가 episodeCount 초과 → 예외
    @Test
    void createWatchParty_회차범위가_episodeCount_초과하면_예외() {
        // given
        Content content = buildTvSeasonContent(10);
        CreateWatchPartyRequest request = buildRequest(1, 15); // episodeCount(10) 초과

        given(userRepository.findById(hostId)).willReturn(Optional.of(host));
        given(contentRepository.findById(contentId)).willReturn(Optional.of(content));

        // when & then
        assertThatThrownBy(() -> watchPartyService.createWatchParty(hostId, request))
                .isInstanceOf(WatchPartyInvalidEpisodeRangeException.class);
    }

    // 케이스 5 (신규): TV_SEASON인데 episodeCount가 null → 통과 (방어 로직 확인)
    @Test
    void createWatchParty_episodeCount가_null이면_초과검증_건너뛰고_통과() {
        // given
        Content content = buildTvSeasonContent(null);
        CreateWatchPartyRequest request = buildRequest(1, 15);

        given(userRepository.findById(hostId)).willReturn(Optional.of(host));
        given(contentRepository.findById(contentId)).willReturn(Optional.of(content));
        given(watchPartyRepository.save(any(WatchParty.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when & then
        assertThatCode(() -> watchPartyService.createWatchParty(hostId, request))
                .doesNotThrowAnyException();
    }
}