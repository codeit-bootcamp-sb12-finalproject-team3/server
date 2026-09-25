package com.moduplaylist.api.content.service.impl;

import com.moduplaylist.api.content.dto.ContentSummaryResponse;
import com.moduplaylist.api.content.service.ContentSummaryResponseAssembler;
import com.moduplaylist.api.content.service.NewContentService;
import com.moduplaylist.api.global.dto.CursorPageResponse;
import com.moduplaylist.api.global.dto.SortDirection;
import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.exception.InvalidContentSearchException;
import com.moduplaylist.core.content.repository.ContentQueryRepository.SearchResult;
import com.moduplaylist.core.content.repository.ContentRepository;
import com.moduplaylist.core.content.repository.NewContentSearch;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NewContentServiceImpl implements NewContentService {

	private static final Duration NEW_CONTENT_WINDOW = Duration.ofDays(30);
	private static final String SORT_BY = "latest";

	private final ContentRepository contentRepository;
	private final ContentSummaryResponseAssembler contentSummaryResponseAssembler;

	@Override
	@Transactional(readOnly = true)
	public CursorPageResponse<ContentSummaryResponse> findNewContents(
		UUID userId,
		String cursor,
		UUID idAfter,
		int limit
	) {
		Instant cursorCreatedAt = parseCursor(cursor);
		NewContentSearch search = new NewContentSearch(
			Instant.now().minus(NEW_CONTENT_WINDOW),
			cursorCreatedAt,
			idAfter,
			limit
		);
		SearchResult result = contentRepository.searchNewContents(search);
		List<Content> contents = result.getContents();
		List<ContentSummaryResponse> data = contentSummaryResponseAssembler
			.toResponses(contents, userId);
		Content lastContent = contents.isEmpty() ? null : contents.get(contents.size() - 1);

		return CursorPageResponse.<ContentSummaryResponse>builder()
			.data(data)
			.nextCursor(result.isHasNext() && lastContent != null
				? lastContent.getCreatedAt().toString()
				: null)
			.nextIdAfter(result.isHasNext() && lastContent != null
				? lastContent.getId()
				: null)
			.hasNext(result.isHasNext())
			.totalCount(result.getTotalCount())
			.sortBy(SORT_BY)
			.sortDirection(SortDirection.DESCENDING)
			.build();
	}

	private Instant parseCursor(String cursor) {
		if (cursor == null || cursor.isBlank()) {
			return null;
		}
		try {
			return Instant.parse(cursor);
		} catch (DateTimeParseException exception) {
			throw new InvalidContentSearchException();
		}
	}
}
