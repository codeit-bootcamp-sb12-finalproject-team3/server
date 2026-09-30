package com.moduplaylist.api.content.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.moduplaylist.core.content.entity.ContentType;
import java.util.Arrays;

public enum ContentTypeFilter {

	MOVIE("movie", ContentType.MOVIE),
	TV_SERIES("tvSeries", ContentType.TV_SEASON),
	SPORT("sport", ContentType.SPORT);

	private final String value;
	private final ContentType queryType;

	ContentTypeFilter(String value, ContentType queryType) {
		this.value = value;
		this.queryType = queryType;
	}

	@JsonValue
	public String getValue() {
		return value;
	}

	public ContentType toQueryType() {
		return queryType;
	}

	@JsonCreator
	public static ContentTypeFilter fromValue(String value) {
		return Arrays.stream(values())
			.filter(type -> type.value.equals(value))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException(
				"지원하지 않는 콘텐츠 타입 필터입니다: " + value));
	}
}
