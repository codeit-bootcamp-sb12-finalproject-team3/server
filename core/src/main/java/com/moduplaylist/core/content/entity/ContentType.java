package com.moduplaylist.core.content.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ContentType {
	MOVIE("movie"),
	TV_SERIES("tvSeries"),
	TV_SEASON("tvSeason"),
	SPORT("sport");

	@JsonValue
	private final String value;

	public boolean isReviewable() {
		return this == MOVIE || this == TV_SEASON || this == SPORT;
	}

	public boolean isLikeable() {
		return isReviewable();
	}

	@JsonCreator
	public static ContentType fromValue(String value) {
		for (ContentType type : values()) {
			if (type.value.equals(value)) {
				return type;
			}
		}
		throw new IllegalArgumentException("지원하지 않는 콘텐츠 타입입니다: " + value);
	}
}
