package com.moduplaylist.api.watchparty.service;

import com.moduplaylist.api.content.dto.ContentWatchPartyResponse;
import com.moduplaylist.api.global.dto.CursorPageResponse;
import com.moduplaylist.api.global.dto.SortDirection;
import com.moduplaylist.api.watchparty.dto.CreateWatchPartyRequest;
import com.moduplaylist.api.watchparty.dto.WatchPartyResponse;
import com.moduplaylist.api.watchparty.dto.WatchPartySummaryResponse;
import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.entity.ContentType;
import com.moduplaylist.core.content.exception.ContentNotFoundException;
import com.moduplaylist.core.user.entity.User;
import com.moduplaylist.core.watchparty.entity.ParticipantStatus;
import com.moduplaylist.core.watchparty.entity.WatchParty;
import com.moduplaylist.core.watchparty.entity.WatchPartyStatus;
import com.moduplaylist.core.watchparty.exception.WatchPartyInvalidEpisodeRangeException;
import com.moduplaylist.core.watchparty.exception.WatchPartyNotFoundException;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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

    // ===== 1. createWatchParty() =====

    // 케이스 1-1: 회차 범위 없음 → 통과
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

    // 케이스 1-2: TV_SEASON, 회차 범위가 episodeCount 이내 → 통과
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

    // 케이스 1-3: 타입 불일치 (MOVIE인데 회차 범위 지정) → 예외
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

    // 케이스 1-4 : 회차 범위가 episodeCount 초과 → 예외
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

    // 케이스 1-5 : TV_SEASON인데 episodeCount가 null → 통과 (방어 로직 확인)
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


    // ===== 2. getWatchParty() =====

    // 케이스 2-1: 정상 조회
    @Test
    void getWatchParty_성공() {
        // given
        WatchParty watchParty = WatchParty.builder()
                .host(host)
                .contentId(contentId)
                .title("파티 제목")
                .description("설명")
                .scheduledAt(Instant.now().plusSeconds(3600))
                .maxParticipants(4)
                .sessionDurationMinutes(120)
                .build();

        Content content = Content.builder()
                .title("영화 제목")
                .type(ContentType.MOVIE)
                .thumbnailUrl("https://example.com/thumb.png")
                .build();

        UUID partyId = UUID.randomUUID();

        given(watchPartyRepository.findById(partyId)).willReturn(Optional.of(watchParty));
        given(contentRepository.findById(contentId)).willReturn(Optional.of(content));
        given(watchPartyParticipantRepository.countByWatchParty_IdAndStatus(
                watchParty.getId(), ParticipantStatus.JOINED)).willReturn(3L);

        // when
        WatchPartyResponse response = watchPartyService.getWatchParty(partyId);

        // then
        assertThat(response.getTitle()).isEqualTo("파티 제목");
    }

    // 케이스 2-2: 존재하지 않으면 예외
    @Test
    void getWatchParty_존재하지않으면_예외() {
        // given
        UUID partyId = UUID.randomUUID();
        given(watchPartyRepository.findById(partyId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> watchPartyService.getWatchParty(partyId))
                .isInstanceOf(WatchPartyNotFoundException.class);
    }


    // ===== 3. getWatchParties() =====

    // 케이스 3-1: contentIdEqual과 statusEqual 동시 지정 → 예외
    @Test
    void getWatchParties_contentIdEqual과_statusEqual_동시지정하면_예외() {
        assertThatThrownBy(() -> watchPartyService.getWatchParties(
                WatchPartyStatus.SCHEDULED, contentId, null, null, 20, SortDirection.ASCENDING))
                .isInstanceOf(BaseException.class);
    }

    // 케이스 3-2: contentIdEqual인데 콘텐츠 없음 → 예외
    @Test
    void getWatchParties_contentIdEqual인데_콘텐츠없으면_예외() {
        given(contentRepository.findByIdAndHiddenFalse(contentId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> watchPartyService.getWatchParties(
                null, contentId, null, null, 20, SortDirection.ASCENDING))
                .isInstanceOf(ContentNotFoundException.class);
    }

    // 케이스 3-3: contentIdEqual 정상 조회
    @Test
    void getWatchParties_contentIdEqual_정상조회() {
        // given
        Content content = Content.builder()
                .title("영화 제목")
                .type(ContentType.MOVIE)
                .thumbnailUrl("https://example.com/thumb.png")
                .build();
        ReflectionTestUtils.setField(content, "id", contentId);

        WatchParty watchParty = WatchParty.builder()
                .host(host)
                .contentId(contentId)
                .title("파티 제목")
                .scheduledAt(Instant.now().plusSeconds(3600))
                .maxParticipants(4)
                .sessionDurationMinutes(120)
                .build();

        given(contentRepository.findByIdAndHiddenFalse(contentId)).willReturn(Optional.of(content));
        given(watchPartyQueryRepository.search(any()))
                .willReturn(new WatchPartyQueryRepository.SearchResult(List.of(watchParty), 1L, false));
        given(contentRepository.findAllById(any())).willReturn(List.of(content));
        given(watchPartyParticipantRepository.countByWatchPartyIdsAndStatus(any(), any()))
                .willReturn(List.of());

        // when
        CursorPageResponse<WatchPartySummaryResponse> response = watchPartyService.getWatchParties(
                null, contentId, null, null, 20, SortDirection.ASCENDING);

        // then
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getTitle()).isEqualTo("파티 제목");
    }

    // 케이스 3-4: statusEqual만 지정하면 콘텐츠 검증 안 함
    @Test
    void getWatchParties_statusEqual만_지정하면_콘텐츠검증_안함() {
        // given
        given(watchPartyQueryRepository.search(any()))
                .willReturn(new WatchPartyQueryRepository.SearchResult(List.of(), 0L, false));

        // when
        CursorPageResponse<WatchPartySummaryResponse> response = watchPartyService.getWatchParties(
                WatchPartyStatus.LIVE, null, null, null, 20, SortDirection.ASCENDING);

        // then
        assertThat(response.getData()).isEmpty();
        verify(contentRepository, never()).findByIdAndHiddenFalse(any());
    }


    // ===== 4. getWatchPartiesForContentWidget() =====

    // 케이스 4-1: 정상 매핑
    @Test
    void getWatchPartiesForContentWidget_정상매핑() {
        // given
        Content content = Content.builder()
                .title("영화 제목")
                .type(ContentType.MOVIE)
                .thumbnailUrl("https://example.com/thumb.png")
                .build();
        ReflectionTestUtils.setField(content, "id", contentId);

        WatchParty watchParty = WatchParty.builder()
                .host(host)
                .contentId(contentId)
                .title("파티 제목")
                .scheduledAt(Instant.now().plusSeconds(3600))
                .maxParticipants(4)
                .sessionDurationMinutes(120)
                .build();

        given(watchPartyQueryRepository.search(any()))
                .willReturn(new WatchPartyQueryRepository.SearchResult(List.of(watchParty), 1L, false));
        given(contentRepository.findAllById(any())).willReturn(List.of(content));
        given(watchPartyParticipantRepository.countByWatchPartyIdsAndStatus(any(), any()))
                .willReturn(List.of());

        // when
        ContentWatchPartyResponse response = watchPartyService.getWatchPartiesForContentWidget(contentId);

        // then
        assertThat(response.getData()).hasSize(1);
        assertThat(response.isHasMore()).isFalse();
    }

    // 케이스 4-2: 결과 없으면 빈 리스트
    @Test
    void getWatchPartiesForContentWidget_결과없으면_빈리스트() {
        // given
        given(watchPartyQueryRepository.search(any()))
                .willReturn(new WatchPartyQueryRepository.SearchResult(List.of(), 0L, false));
        given(contentRepository.findAllById(any())).willReturn(List.of());

        // when
        ContentWatchPartyResponse response = watchPartyService.getWatchPartiesForContentWidget(contentId);

        // then
        assertThat(response.getData()).isEmpty();
    }
}