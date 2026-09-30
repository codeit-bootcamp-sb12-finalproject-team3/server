package com.moduplaylist.api.trending.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.moduplaylist.api.content.dto.ContentSummaryResponse;
import com.moduplaylist.api.content.service.ContentSummaryResponseAssembler;
import com.moduplaylist.api.global.dto.CursorPageResponse;
import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.entity.ContentType;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.infrastructure.redis.trending.TrendingContentRedisRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrendingServiceImplTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID INTERNAL_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Mock
    private TrendingContentRedisRepository trendingRedisRepository;

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private ContentSummaryResponseAssembler contentSummaryResponseAssembler;

    @Mock
    private Content content;

    @Mock
    private ContentSummaryResponse response;

    private TrendingServiceImpl trendingService;

    @BeforeEach
    void setUp() {
        trendingService = new TrendingServiceImpl(
                trendingRedisRepository,
                contentRepository,
                contentSummaryResponseAssembler
        );
    }

    @Test
    void internalTrendingResultIsReturned() {
        when(trendingRedisRepository.findTopContentIds(
                org.mockito.ArgumentMatchers.eq(100),
                org.mockito.ArgumentMatchers.any(Instant.class)
        )).thenReturn(List.of(INTERNAL_ID));
        when(contentRepository.findAllById(List.of(INTERNAL_ID))).thenReturn(List.of(content));
        when(content.getId()).thenReturn(INTERNAL_ID);
        when(content.isPubliclyVisible()).thenReturn(true);
        when(content.getType()).thenReturn(ContentType.MOVIE);
        when(contentSummaryResponseAssembler.toResponses(List.of(content), USER_ID))
                .thenReturn(List.of(response));

        CursorPageResponse<ContentSummaryResponse> result =
                trendingService.findTrendingContents(USER_ID);

        assertThat(result.getData()).containsExactly(response);
    }

    @Test
    void emptyInternalTrendingResultReturnsEmptyPage() {
        when(trendingRedisRepository.findTopContentIds(
                org.mockito.ArgumentMatchers.eq(100),
                org.mockito.ArgumentMatchers.any(Instant.class)
        )).thenReturn(List.of());
        when(contentSummaryResponseAssembler.toResponses(List.of(), USER_ID)).thenReturn(List.of());

        CursorPageResponse<ContentSummaryResponse> result =
                trendingService.findTrendingContents(USER_ID);

        assertEmptyPage(result);
    }

    @Test
    void missingInternalContentsReturnEmptyPage() {
        when(trendingRedisRepository.findTopContentIds(
                org.mockito.ArgumentMatchers.eq(100),
                org.mockito.ArgumentMatchers.any(Instant.class)
        )).thenReturn(List.of(INTERNAL_ID));
        when(contentRepository.findAllById(List.of(INTERNAL_ID))).thenReturn(List.of());
        when(contentSummaryResponseAssembler.toResponses(List.of(), USER_ID)).thenReturn(List.of());

        CursorPageResponse<ContentSummaryResponse> result =
                trendingService.findTrendingContents(USER_ID);

        assertEmptyPage(result);
    }

    @Test
    void hiddenInternalContentReturnsEmptyPage() {
        when(trendingRedisRepository.findTopContentIds(
                org.mockito.ArgumentMatchers.eq(100),
                org.mockito.ArgumentMatchers.any(Instant.class)
        )).thenReturn(List.of(INTERNAL_ID));
        when(contentRepository.findAllById(List.of(INTERNAL_ID))).thenReturn(List.of(content));
        when(content.getId()).thenReturn(INTERNAL_ID);
        when(content.isPubliclyVisible()).thenReturn(false);
        when(contentSummaryResponseAssembler.toResponses(List.of(), USER_ID)).thenReturn(List.of());

        CursorPageResponse<ContentSummaryResponse> result =
                trendingService.findTrendingContents(USER_ID);

        assertEmptyPage(result);
    }

    @Test
    void unsupportedInternalContentTypeReturnsEmptyPage() {
        when(trendingRedisRepository.findTopContentIds(
                org.mockito.ArgumentMatchers.eq(100),
                org.mockito.ArgumentMatchers.any(Instant.class)
        )).thenReturn(List.of(INTERNAL_ID));
        when(contentRepository.findAllById(List.of(INTERNAL_ID))).thenReturn(List.of(content));
        when(content.getId()).thenReturn(INTERNAL_ID);
        when(content.isPubliclyVisible()).thenReturn(true);
        when(content.getType()).thenReturn(ContentType.TV_SERIES);
        when(contentSummaryResponseAssembler.toResponses(List.of(), USER_ID)).thenReturn(List.of());

        CursorPageResponse<ContentSummaryResponse> result =
                trendingService.findTrendingContents(USER_ID);

        assertEmptyPage(result);
    }

    private static void assertEmptyPage(CursorPageResponse<ContentSummaryResponse> result) {
        assertThat(result.getData()).isEmpty();
        assertThat(result.getTotalCount()).isZero();
        assertThat(result.getHasNext()).isFalse();
        assertThat(result.getNextCursor()).isNull();
        assertThat(result.getNextIdAfter()).isNull();
    }
}
