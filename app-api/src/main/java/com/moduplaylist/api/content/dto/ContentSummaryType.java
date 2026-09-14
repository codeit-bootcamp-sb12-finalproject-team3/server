package com.moduplaylist.api.content.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.moduplaylist.core.content.entity.ContentType;
import java.util.Arrays;

public enum ContentSummaryType {

	MOVIE("movie", ContentType.MOVIE),
	TV_SEASON("tvSeason", ContentType.TV_SEASON),
	SPORT("sport", ContentType.SPORT);

	private final String value;
	private final ContentType contentType;

	ContentSummaryType(String value, ContentType contentType) {
		this.value = value;
		this.contentType = contentType;
	}

	@JsonValue
	public String getValue() {
		return value;
	}

	public ContentType toContentType() {
		return contentType;
	}

	public static ContentSummaryType from(ContentType contentType) {
		return Arrays.stream(values())
			.filter(type -> type.contentType == contentType)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException(
				"목록에 노출할 수 없는 콘텐츠 타입입니다: " + contentType));
	}

	@JsonCreator
	public static ContentSummaryType fromValue(String value) {
		return Arrays.stream(values())
			.filter(type -> type.value.equals(value))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException(
				"지원하지 않는 콘텐츠 요약 타입입니다: " + value));
	}
}
