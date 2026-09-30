package com.moduplaylist.api.content.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Locale;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ContentSearchRequest {

	@Size(max = 100)
	private String keywordLike;

	private ContentTypeFilter typeEqual;

	private UUID genreIdEqual;

	@Size(max = 50)
	private String sportTypeEqual;

	private Boolean likedByMe;

	private UUID likedByUserIdEqual;

	private ContentSort sortBy;

	@Size(max = 2048)
	private String cursor;

	private UUID idAfter;

	@Min(1)
	@Max(100)
	@NotNull
	private Integer limit = 20;

	public void setKeywordLike(String value) {
		this.keywordLike = normalize(value);
	}

	public void setSportTypeEqual(String value) {
		String normalized = normalize(value);
		this.sportTypeEqual = normalized == null
			? null
			: normalized.toUpperCase(Locale.ROOT);
	}

	@JsonIgnore
	@AssertTrue(message = "cursor와 idAfter는 함께 전달해야 합니다.")
	public boolean isCursorValid() {
		boolean cursorEmpty = cursor == null || cursor.isBlank();
		return cursorEmpty == (idAfter == null);
	}

	@JsonIgnore
	@AssertTrue(message = "콘텐츠 타입과 장르·스포츠 종목·검색어 조건의 조합이 올바르지 않습니다.")
	public boolean isFilterCombinationValid() {
		boolean hasKeyword = keywordLike != null && !keywordLike.isBlank();
		boolean hasSportType = sportTypeEqual != null && !sportTypeEqual.isBlank();

		boolean likedContentsSearch =
				Boolean.TRUE.equals(likedByMe) || likedByUserIdEqual != null;

		if (Boolean.TRUE.equals(likedByMe) && likedByUserIdEqual != null) {
			return false;
		}

		if (likedContentsSearch && sortBy != null) {
			return false;
		}

		if (hasKeyword && likedContentsSearch) {
			return false;
		}
		if (hasKeyword && (genreIdEqual != null || hasSportType)) {
			return false;
		}
		if (genreIdEqual != null) {
			return typeEqual == ContentTypeFilter.MOVIE
				|| typeEqual == ContentTypeFilter.TV_SERIES;
		}
		if (hasSportType) {
			return typeEqual == ContentTypeFilter.SPORT;
		}
		return true;
	}

	private static String normalize(String value) {
		return value == null ? null : value.strip();
	}
}
