package com.moduplaylist.api.content.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.moduplaylist.api.content.dto.ContentSummaryResponse;
import com.moduplaylist.api.content.service.ContentSummaryResponseAssembler;
import com.moduplaylist.api.global.dto.CursorPageResponse;
import com.moduplaylist.api.global.dto.SortDirection;
import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.exception.InvalidContentSearchException;
import com.moduplaylist.core.content.repository.ContentQueryRepository.SearchResult;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.core.content.repository.NewContentSearch;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NewContentServiceImplTest {

	private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID CONTENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

	@Mock
	private ContentRepository contentRepository;

	@Mock
	private ContentSummaryResponseAssembler contentSummaryResponseAssembler;

	@Mock
	private Content content;

	@Mock
	private ContentSummaryResponse response;

	private NewContentServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new NewContentServiceImpl(contentRepository, contentSummaryResponseAssembler);
	}

	@Test
	void recentContentsAreReturnedWithLatestCursorPage() {
		Instant contentCreatedAt = Instant.parse("2026-09-23T01:00:00Z");
		when(contentRepository.searchNewContents(any(NewContentSearch.class)))
			.thenReturn(new SearchResult(List.of(content), 2L, true, null));
		when(contentSummaryResponseAssembler.toResponses(List.of(content), USER_ID))
			.thenReturn(List.of(response));
		when(content.getCreatedAt()).thenReturn(contentCreatedAt);
		when(content.getId()).thenReturn(CONTENT_ID);
		Instant before = Instant.now().minus(Duration.ofDays(30));

		CursorPageResponse<ContentSummaryResponse> result =
			service.findNewContents(USER_ID, null, null, 1);

		Instant after = Instant.now().minus(Duration.ofDays(30));
		ArgumentCaptor<NewContentSearch> captor = ArgumentCaptor.forClass(NewContentSearch.class);
		org.mockito.Mockito.verify(contentRepository).searchNewContents(captor.capture());
		assertThat(captor.getValue().getCreatedAtFrom()).isBetween(before, after);
		assertThat(captor.getValue().getLimit()).isEqualTo(1);
		assertThat(result.getData()).containsExactly(response);
		assertThat(result.getNextCursor()).isEqualTo(contentCreatedAt.toString());
		assertThat(result.getNextIdAfter()).isEqualTo(CONTENT_ID);
		assertThat(result.getHasNext()).isTrue();
		assertThat(result.getTotalCount()).isEqualTo(2L);
		assertThat(result.getSortBy()).isEqualTo("latest");
		assertThat(result.getSortDirection()).isEqualTo(SortDirection.DESCENDING);
	}

	@Test
	void cursorIsParsedForNextPage() {
		Instant cursor = Instant.parse("2026-09-22T12:00:00Z");
		when(contentRepository.searchNewContents(any(NewContentSearch.class)))
			.thenReturn(new SearchResult(List.of(), 0L, false, null));
		when(contentSummaryResponseAssembler.toResponses(List.of(), USER_ID)).thenReturn(List.of());

		service.findNewContents(USER_ID, cursor.toString(), CONTENT_ID, 20);

		ArgumentCaptor<NewContentSearch> captor = ArgumentCaptor.forClass(NewContentSearch.class);
		org.mockito.Mockito.verify(contentRepository).searchNewContents(captor.capture());
		assertThat(captor.getValue().getCursorCreatedAt()).isEqualTo(cursor);
		assertThat(captor.getValue().getIdAfter()).isEqualTo(CONTENT_ID);
	}

	@Test
	void emptyResultReturnsEmptyPage() {
		when(contentRepository.searchNewContents(any(NewContentSearch.class)))
			.thenReturn(new SearchResult(List.of(), 0L, false, null));
		when(contentSummaryResponseAssembler.toResponses(List.of(), USER_ID)).thenReturn(List.of());

		CursorPageResponse<ContentSummaryResponse> result =
			service.findNewContents(USER_ID, null, null, 20);

		assertThat(result.getData()).isEmpty();
		assertThat(result.getNextCursor()).isNull();
		assertThat(result.getNextIdAfter()).isNull();
		assertThat(result.getHasNext()).isFalse();
		assertThat(result.getTotalCount()).isZero();
	}

	@Test
	void invalidCursorIsRejected() {
		assertThatThrownBy(() -> service.findNewContents(
			USER_ID,
			"invalid-cursor",
			CONTENT_ID,
			20
		))
			.isInstanceOf(InvalidContentSearchException.class);
		verifyNoInteractions(contentRepository, contentSummaryResponseAssembler);
	}
}
