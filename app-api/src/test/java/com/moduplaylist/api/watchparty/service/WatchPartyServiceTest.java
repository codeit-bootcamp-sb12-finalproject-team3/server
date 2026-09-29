package com.moduplaylist.api.watchparty.service;

import com.moduplaylist.api.content.dto.ContentWatchPartyResponse;
import com.moduplaylist.api.global.dto.CursorPageResponse;
import com.moduplaylist.api.global.dto.SortDirection;
import com.moduplaylist.api.watchparty.dto.CreateWatchPartyRequest;
import com.moduplaylist.api.watchparty.dto.UpdateWatchPartyRequest;
import com.moduplaylist.api.watchparty.dto.WatchPartyResponse;
import com.moduplaylist.api.watchparty.dto.WatchPartySummaryResponse;
import com.moduplaylist.api.watchparty.event.WatchPartyCancelledEvent;
import com.moduplaylist.api.watchparty.event.WatchPartyUpdatedEvent;
import com.moduplaylist.core.common.exception.BaseException;
import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.entity.ContentType;
import com.moduplaylist.core.content.exception.ContentNotFoundException;
import com.moduplaylist.core.user.entity.User;
import com.moduplaylist.core.watchparty.entity.ParticipantStatus;
import com.moduplaylist.core.watchparty.entity.WatchParty;
import com.moduplaylist.core.watchparty.entity.WatchPartyParticipant;
import com.moduplaylist.core.watchparty.entity.WatchPartyStatus;
import com.moduplaylist.core.watchparty.exception.*;
import com.moduplaylist.core.watchparty.repository.*;
import com.moduplaylist.core.user.repository.UserRepository;
import com.moduplaylist.core.content.repository.ContentRepository;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

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

    private WatchParty buildWatchParty(WatchPartyStatus status) {
        ReflectionTestUtils.setField(host, "id", hostId);
        WatchParty watchParty = WatchParty.builder()
                .host(host)
                .contentId(contentId)
                .title("기존 제목")
                .description("기존 설명")
                .scheduledAt(Instant.now().plusSeconds(3600))
                .maxParticipants(4)
                .sessionDurationMinutes(120)
                .build();
        ReflectionTestUtils.setField(watchParty, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(watchParty, "status", status);
        return watchParty;
    }

    private UpdateWatchPartyRequest buildUpdateRequest(Integer startEpisode, Integer endEpisode) {
        return new UpdateWatchPartyRequest(
                "수정된 제목", "수정된 설명", Instant.now().plusSeconds(7200),
                6, 90, startEpisode, endEpisode
        );
    }

    private CreateWatchPartyRequest buildRequestWithMax(int maxParticipants) {
        return new CreateWatchPartyRequest(
                contentId, "파티 제목", null, Instant.now().plusSeconds(3600),
                maxParticipants, 120, null, null
        );
    }

    private Content buildMovieContent() {
        return Content.builder()
                .title("영화 콘텐츠").type(ContentType.MOVIE)
                .thumbnailUrl("https://example.com/thumb.png").build();
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


    // ===== 5. updateWatchParty() =====

    // 케이스 5-1: 정상 수정
    @Test
    void updateWatchParty_정상_수정() {
        WatchParty watchParty = buildWatchParty(WatchPartyStatus.SCHEDULED);
        UpdateWatchPartyRequest request = buildUpdateRequest(null, null);
        Content movieContent = Content.builder()
                .title("영화 콘텐츠").type(ContentType.MOVIE)
                .thumbnailUrl("https://example.com/thumb.png").build();

        given(watchPartyRepository.findById(watchParty.getId())).willReturn(Optional.of(watchParty));
        given(contentRepository.findById(contentId)).willReturn(Optional.of(movieContent));
        given(watchPartyParticipantRepository.countByWatchParty_IdAndStatus(any(), any())).willReturn(0L);

        WatchPartyResponse response = watchPartyService.updateWatchParty(hostId, watchParty.getId(), request);

        assertThat(response.getTitle()).isEqualTo("수정된 제목");
        assertThat(watchParty.getMaxParticipants()).isEqualTo(6);
    }

    // 케이스 5-2: host 아니면 예외
    @Test
    void updateWatchParty_host_아니면_예외() {
        WatchParty watchParty = buildWatchParty(WatchPartyStatus.SCHEDULED);
        UpdateWatchPartyRequest request = buildUpdateRequest(null, null);
        UUID otherUserId = UUID.randomUUID();

        given(watchPartyRepository.findById(watchParty.getId())).willReturn(Optional.of(watchParty));

        assertThatThrownBy(() -> watchPartyService.updateWatchParty(otherUserId, watchParty.getId(), request))
                .isInstanceOf(WatchPartyHostOnlyException.class);

        verify(contentRepository, never()).findById(any());
    }

    // 케이스 5-3: SCHEDULED 아니면 예외
    @Test
    void updateWatchParty_SCHEDULED_아니면_예외() {
        WatchParty watchParty = buildWatchParty(WatchPartyStatus.LIVE);
        UpdateWatchPartyRequest request = buildUpdateRequest(null, null);
        Content movieContent = Content.builder()
                .title("영화 콘텐츠").type(ContentType.MOVIE)
                .thumbnailUrl("https://example.com/thumb.png").build();

        given(watchPartyRepository.findById(watchParty.getId())).willReturn(Optional.of(watchParty));
        given(contentRepository.findById(contentId)).willReturn(Optional.of(movieContent));

        assertThatThrownBy(() -> watchPartyService.updateWatchParty(hostId, watchParty.getId(), request))
                .isInstanceOf(WatchPartyInvalidStateException.class);
    }

    // 케이스 5-4: 회차 범위 초과 예외
    @Test
    void updateWatchParty_회차범위_초과하면_예외() {
        WatchParty watchParty = buildWatchParty(WatchPartyStatus.SCHEDULED);
        UpdateWatchPartyRequest request = buildUpdateRequest(1, 20);
        Content tvSeasonContent = buildTvSeasonContent(10);

        given(watchPartyRepository.findById(watchParty.getId())).willReturn(Optional.of(watchParty));
        given(contentRepository.findById(contentId)).willReturn(Optional.of(tvSeasonContent));

        assertThatThrownBy(() -> watchPartyService.updateWatchParty(hostId, watchParty.getId(), request))
                .isInstanceOf(WatchPartyInvalidEpisodeRangeException.class);
    }

    // 케이스 5-5: 파티 없으면 예외
    @Test
    void updateWatchParty_존재하지않으면_예외() {
        UUID unknownPartyId = UUID.randomUUID();
        UpdateWatchPartyRequest request = buildUpdateRequest(null, null);

        given(watchPartyRepository.findById(unknownPartyId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> watchPartyService.updateWatchParty(hostId, unknownPartyId, request))
                .isInstanceOf(WatchPartyNotFoundException.class);
    }

    // 케이스 5-6: 현재 참가자 수보다 작은 maxParticipants로 수정 시도 → 예외
    @Test
    void updateWatchParty_현재참가자수보다_작은_maxParticipants_요청시_예외() {
        WatchParty watchParty = buildWatchParty(WatchPartyStatus.SCHEDULED);
        Content movieContent = Content.builder()
                .title("영화 콘텐츠").type(ContentType.MOVIE)
                .thumbnailUrl("https://example.com/thumb.png").build();
        UpdateWatchPartyRequest request = new UpdateWatchPartyRequest(
                "수정된 제목", "수정된 설명", Instant.now().plusSeconds(7200),
                2, 90, null, null   // maxParticipants=2
        );

        given(watchPartyRepository.findById(watchParty.getId())).willReturn(Optional.of(watchParty));
        given(contentRepository.findById(contentId)).willReturn(Optional.of(movieContent));
        given(watchPartyParticipantRepository.countByWatchParty_IdAndStatus(watchParty.getId(), ParticipantStatus.JOINED))
                .willReturn(4L);   // 현재 4명 참가 중

        assertThatThrownBy(() -> watchPartyService.updateWatchParty(hostId, watchParty.getId(), request))
                .isInstanceOf(WatchPartyMaxParticipantsBelowCurrentException.class);
    }

    // 케이스 5-7: 현재 참가자 수와 같거나 크면 통과
    @Test
    void updateWatchParty_현재참가자수_이상이면_통과() {
        WatchParty watchParty = buildWatchParty(WatchPartyStatus.SCHEDULED);
        Content movieContent = Content.builder()
                .title("영화 콘텐츠").type(ContentType.MOVIE)
                .thumbnailUrl("https://example.com/thumb.png").build();
        UpdateWatchPartyRequest request = new UpdateWatchPartyRequest(
                "수정된 제목", "수정된 설명", Instant.now().plusSeconds(7200),
                4, 90, null, null   // maxParticipants=4, 현재 인원과 동일
        );

        given(watchPartyRepository.findById(watchParty.getId())).willReturn(Optional.of(watchParty));
        given(contentRepository.findById(contentId)).willReturn(Optional.of(movieContent));
        given(watchPartyParticipantRepository.countByWatchParty_IdAndStatus(watchParty.getId(), ParticipantStatus.JOINED))
                .willReturn(4L);

        watchPartyService.updateWatchParty(hostId, watchParty.getId(), request);

        assertThat(watchParty.getMaxParticipants()).isEqualTo(4);
    }

    // ===== 6. 정원 상한 (MAX_PARTICIPANTS_LIMIT, #177) =====

    // 케이스 6-1: 생성 시 상한과 같으면(경계값) 통과
    @Test
    void createWatchParty_maxParticipants가_상한과_같으면_통과() {
        CreateWatchPartyRequest request = buildRequestWithMax(WatchPartyService.MAX_PARTICIPANTS_LIMIT);

        given(userRepository.findById(hostId)).willReturn(Optional.of(host));
        given(contentRepository.findById(contentId)).willReturn(Optional.of(buildMovieContent()));
        given(watchPartyRepository.save(any(WatchParty.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> watchPartyService.createWatchParty(hostId, request))
                .doesNotThrowAnyException();
    }

    // 케이스 6-2: 생성 시 상한 초과 → 예외, 저장 안 함
    @Test
    void createWatchParty_maxParticipants가_상한초과면_예외() {
        CreateWatchPartyRequest request = buildRequestWithMax(WatchPartyService.MAX_PARTICIPANTS_LIMIT + 1);

        given(userRepository.findById(hostId)).willReturn(Optional.of(host));
        given(contentRepository.findById(contentId)).willReturn(Optional.of(buildMovieContent()));

        assertThatThrownBy(() -> watchPartyService.createWatchParty(hostId, request))
                .isInstanceOf(WatchPartyMaxParticipantsExceededException.class)
                .satisfies(e -> assertThat(((BaseException) e).getData())
                        .containsEntry("limit", WatchPartyService.MAX_PARTICIPANTS_LIMIT)
                        .containsEntry("requestedMaxParticipants", WatchPartyService.MAX_PARTICIPANTS_LIMIT + 1));

        verify(watchPartyRepository, never()).save(any());
    }

    // 케이스 6-3: 상한 도입 전에 이미 상한을 넘게 만들어진 방 — 정원은 그대로 두고 다른 필드만 수정하면 통과
    //   (수정 API는 전체 필드를 받으므로 기존 정원이 그대로 넘어와도 막히면 안 됨. 상한 검사는 정원 값이 바뀔 때만)
    @Test
    void updateWatchParty_기존값이_상한초과여도_안바꾸면_통과() {
        WatchParty watchParty = buildWatchParty(WatchPartyStatus.SCHEDULED);
        int legacyMax = WatchPartyService.MAX_PARTICIPANTS_LIMIT + 20;   // 상한 도입 전에 만든 초과 방
        ReflectionTestUtils.setField(watchParty, "maxParticipants", legacyMax);
        UpdateWatchPartyRequest request = new UpdateWatchPartyRequest(
                "제목만 수정", "설명", Instant.now().plusSeconds(7200),
                legacyMax, 90, null, null   // 정원은 그대로
        );

        given(watchPartyRepository.findById(watchParty.getId())).willReturn(Optional.of(watchParty));
        given(contentRepository.findById(contentId)).willReturn(Optional.of(buildMovieContent()));
        given(watchPartyParticipantRepository.countByWatchParty_IdAndStatus(watchParty.getId(), ParticipantStatus.JOINED))
                .willReturn(0L);

        WatchPartyResponse response = watchPartyService.updateWatchParty(hostId, watchParty.getId(), request);

        assertThat(response.getTitle()).isEqualTo("제목만 수정");
        assertThat(watchParty.getMaxParticipants()).isEqualTo(legacyMax);
    }

    // 케이스 6-4: 수정으로 정원을 상한 초과 값으로 바꾸면 → 예외, 변경 안 됨
    @Test
    void updateWatchParty_정원을_상한초과로_바꾸면_예외() {
        WatchParty watchParty = buildWatchParty(WatchPartyStatus.SCHEDULED);   // 기존 maxParticipants=4
        UpdateWatchPartyRequest request = new UpdateWatchPartyRequest(
                "수정된 제목", "수정된 설명", Instant.now().plusSeconds(7200),
                WatchPartyService.MAX_PARTICIPANTS_LIMIT + 1, 90, null, null
        );

        given(watchPartyRepository.findById(watchParty.getId())).willReturn(Optional.of(watchParty));
        given(contentRepository.findById(contentId)).willReturn(Optional.of(buildMovieContent()));

        assertThatThrownBy(() -> watchPartyService.updateWatchParty(hostId, watchParty.getId(), request))
                .isInstanceOf(WatchPartyMaxParticipantsExceededException.class);

        assertThat(watchParty.getMaxParticipants()).isEqualTo(4);
        verify(watchPartyParticipantRepository, never()).countByWatchParty_IdAndStatus(any(), any());
    }


    // ===== 7. deleteWatchParty() =====

    // 케이스 7-1: 정상 삭제
    @Test
    void deleteWatchParty_정상_삭제() {
        WatchParty watchParty = buildWatchParty(WatchPartyStatus.SCHEDULED);
        given(watchPartyRepository.findById(watchParty.getId())).willReturn(Optional.of(watchParty));

        watchPartyService.deleteWatchParty(hostId, watchParty.getId());

        verify(watchPartyRepository).delete(watchParty);
        verify(watchPartyHostRegistry).removeHost(watchParty.getId());
    }

    // 케이스 7-2: host 아니면 예외
    @Test
    void deleteWatchParty_host_아니면_예외() {
        WatchParty watchParty = buildWatchParty(WatchPartyStatus.SCHEDULED);
        UUID otherUserId = UUID.randomUUID();
        given(watchPartyRepository.findById(watchParty.getId())).willReturn(Optional.of(watchParty));

        assertThatThrownBy(() -> watchPartyService.deleteWatchParty(otherUserId, watchParty.getId()))
                .isInstanceOf(WatchPartyHostOnlyException.class);

        verify(watchPartyRepository, never()).delete(any());
        verify(watchPartyHostRegistry, never()).removeHost(any());
    }

    // 케이스 7-3: SCHEDULED 아니면 예외
    @Test
    void deleteWatchParty_SCHEDULED_아니면_예외() {
        WatchParty watchParty = buildWatchParty(WatchPartyStatus.ENDED);
        given(watchPartyRepository.findById(watchParty.getId())).willReturn(Optional.of(watchParty));

        assertThatThrownBy(() -> watchPartyService.deleteWatchParty(hostId, watchParty.getId()))
                .isInstanceOf(WatchPartyInvalidStateException.class);

        verify(watchPartyRepository, never()).delete(any());
        verify(watchPartyHostRegistry, never()).removeHost(any());
    }

    // 케이스 7-4: 파티 없으면 예외
    @Test
    void deleteWatchParty_존재하지않으면_예외() {
        UUID unknownPartyId = UUID.randomUUID();
        given(watchPartyRepository.findById(unknownPartyId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> watchPartyService.deleteWatchParty(hostId, unknownPartyId))
                .isInstanceOf(WatchPartyNotFoundException.class);
    }

    // ===== 8. 변경·취소 알림 이벤트  =====

    private User buildUser(UUID userId) {
        User user = User.create(userId + "@test.com", "encodedPw", "참가자");
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }

    // 케이스 8-1: 시작 시각이 바뀌면 → 참가자 + 알림 설정자(중복 제거)에게 보낼 변경 이벤트 발행
    @Test
    void updateWatchParty_시작시각_변경시_변경이벤트_발행() {
        WatchParty watchParty = buildWatchParty(WatchPartyStatus.SCHEDULED);
        Instant previousScheduledAt = watchParty.getScheduledAt();
        UpdateWatchPartyRequest request = buildUpdateRequest(null, null);   // scheduledAt: now + 2시간

        UUID joinedAndReminded = UUID.randomUUID();   // 참가 중이면서 알림도 설정한 사람
        UUID remindedOnly = UUID.randomUUID();        // 알림만 설정한 사람
        WatchPartyParticipant participant = new WatchPartyParticipant(buildUser(joinedAndReminded), watchParty);

        given(watchPartyRepository.findById(watchParty.getId())).willReturn(Optional.of(watchParty));
        given(contentRepository.findById(contentId)).willReturn(Optional.of(buildMovieContent()));
        given(watchPartyParticipantRepository.countByWatchParty_IdAndStatus(any(), any())).willReturn(1L);
        given(watchPartyParticipantRepository.findJoinedParticipants(watchParty.getId(), ParticipantStatus.JOINED))
                .willReturn(List.of(participant));
        given(watchPartyReminderRepository.findUserIdsByWatchPartyId(watchParty.getId()))
                .willReturn(List.of(joinedAndReminded, remindedOnly));

        watchPartyService.updateWatchParty(hostId, watchParty.getId(), request);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        WatchPartyUpdatedEvent event = (WatchPartyUpdatedEvent) captor.getValue();

        assertThat(event.partyId()).isEqualTo(watchParty.getId());
        assertThat(event.title()).isEqualTo("수정된 제목");
        assertThat(event.previousScheduledAt()).isEqualTo(previousScheduledAt);
        assertThat(event.scheduledAt()).isEqualTo(request.getScheduledAt());
        assertThat(event.recipientIds()).containsExactly(joinedAndReminded, remindedOnly);   // 중복 제거
    }

    // 케이스 8-2: 시작 시각이 그대로면(제목만 수정) → 이벤트 발행 없음, 받는 사람 조회도 안 함
    @Test
    void updateWatchParty_시작시각_그대로면_이벤트_없음() {
        WatchParty watchParty = buildWatchParty(WatchPartyStatus.SCHEDULED);
        UpdateWatchPartyRequest request = new UpdateWatchPartyRequest(
                "제목만 수정", "설명", watchParty.getScheduledAt(),   // 시작 시각 그대로
                4, 90, null, null
        );

        given(watchPartyRepository.findById(watchParty.getId())).willReturn(Optional.of(watchParty));
        given(contentRepository.findById(contentId)).willReturn(Optional.of(buildMovieContent()));
        given(watchPartyParticipantRepository.countByWatchParty_IdAndStatus(any(), any())).willReturn(0L);

        watchPartyService.updateWatchParty(hostId, watchParty.getId(), request);

        verifyNoInteractions(eventPublisher, watchPartyReminderRepository);
        verify(watchPartyParticipantRepository, never()).findJoinedParticipants(any(), any());
    }

    // 케이스 8-3: 삭제 → 삭제 전에 제목·시각·받는 사람을 캡처해 취소 이벤트 발행
    @Test
    void deleteWatchParty_삭제전_캡처해서_취소이벤트_발행() {
        WatchParty watchParty = buildWatchParty(WatchPartyStatus.SCHEDULED);
        UUID joinedUserId = UUID.randomUUID();
        UUID remindedUserId = UUID.randomUUID();
        WatchPartyParticipant participant = new WatchPartyParticipant(buildUser(joinedUserId), watchParty);

        given(watchPartyRepository.findById(watchParty.getId())).willReturn(Optional.of(watchParty));
        given(watchPartyParticipantRepository.findJoinedParticipants(watchParty.getId(), ParticipantStatus.JOINED))
                .willReturn(List.of(participant));
        given(watchPartyReminderRepository.findUserIdsByWatchPartyId(watchParty.getId()))
                .willReturn(List.of(remindedUserId));

        watchPartyService.deleteWatchParty(hostId, watchParty.getId());

        // cascade로 사라지기 전에 조회해야 함 → 조회 2개가 delete보다 먼저
        InOrder inOrder = inOrder(watchPartyParticipantRepository, watchPartyReminderRepository, watchPartyRepository);
        inOrder.verify(watchPartyParticipantRepository).findJoinedParticipants(watchParty.getId(), ParticipantStatus.JOINED);
        inOrder.verify(watchPartyReminderRepository).findUserIdsByWatchPartyId(watchParty.getId());
        inOrder.verify(watchPartyRepository).delete(watchParty);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        WatchPartyCancelledEvent event = (WatchPartyCancelledEvent) captor.getValue();

        assertThat(event.partyId()).isEqualTo(watchParty.getId());
        assertThat(event.title()).isEqualTo("기존 제목");
        assertThat(event.scheduledAt()).isEqualTo(watchParty.getScheduledAt());
        assertThat(event.recipientIds()).containsExactly(joinedUserId, remindedUserId);
    }

    // 케이스 8-4: 삭제 권한이 없으면 → 취소 이벤트 없음 (삭제도 안 됨)
    @Test
    void deleteWatchParty_host_아니면_취소이벤트_없음() {
        WatchParty watchParty = buildWatchParty(WatchPartyStatus.SCHEDULED);
        given(watchPartyRepository.findById(watchParty.getId())).willReturn(Optional.of(watchParty));

        assertThatThrownBy(() -> watchPartyService.deleteWatchParty(UUID.randomUUID(), watchParty.getId()))
                .isInstanceOf(WatchPartyHostOnlyException.class);

        verifyNoInteractions(eventPublisher);
    }

}